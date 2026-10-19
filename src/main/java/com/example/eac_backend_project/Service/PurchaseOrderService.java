package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.dto.PurchaseOrderDTO;
import com.example.eac_backend_project.Service.email.EmailService;
import com.example.eac_backend_project.dto.StoreReceiptDTO;
import com.example.eac_backend_project.entity.inventory.PurchaseOrder;
import com.example.eac_backend_project.entity.inventory.PurchaseOrderItem;
import com.example.eac_backend_project.entity.inventory.Product;
import com.example.eac_backend_project.entity.inventory.StoreReceipt;
import com.example.eac_backend_project.repository.PurchaseOrderRepository;
import com.example.eac_backend_project.repository.ProductRepository;
import com.example.eac_backend_project.repository.StoreReceiptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ProductRepository productRepository;
    private final InventoryRequestService inventoryRequestService;
    private final EmailService emailService;

    @Autowired
    private StoreReceiptRepository storeReceiptRepository;

    public PurchaseOrderService(PurchaseOrderRepository purchaseOrderRepository,
                                ProductRepository productRepository,
                                InventoryRequestService inventoryRequestService,
                                EmailService emailService) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.productRepository = productRepository;
        this.inventoryRequestService = inventoryRequestService;
        this.emailService = emailService;
    }

    @Transactional
    public PurchaseOrder createPurchaseOrder(PurchaseOrderDTO dto, String orderedBy) {
        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setSupplierName(dto.getSupplierName());
        purchaseOrder.setSupplierContact(dto.getSupplierContact());
        purchaseOrder.setOrderDate(dto.getOrderDate() != null ? dto.getOrderDate() : LocalDateTime.now());
        purchaseOrder.setExpectedDeliveryDate(dto.getExpectedDeliveryDate());
        purchaseOrder.setDeliveryAddress(dto.getDeliveryAddress());
        purchaseOrder.setNotes(dto.getNotes());
        purchaseOrder.setOrderedBy(orderedBy);
        purchaseOrder.setStatus("ORDERED");
        purchaseOrder.setStoreNotified(false);

        double totalAmount = 0.0;

        if (dto.getItems() != null) {
            for (PurchaseOrderDTO.PurchaseItemDTO itemDTO : dto.getItems()) {
                PurchaseOrderItem item = new PurchaseOrderItem();
                item.setPurchaseOrder(purchaseOrder);

                if (itemDTO.getIsNewProduct() != null && itemDTO.getIsNewProduct()) {
                    // Create new product if it doesn't exist
                    Product newProduct = new Product();
                    newProduct.setName(itemDTO.getProductName());
                    newProduct.setCode(itemDTO.getProductCode());
                    newProduct.setDescription("Purchased from " + dto.getSupplierName());
                    newProduct.setStock(0); // Will be added when received
                    newProduct.setEntryDate(LocalDateTime.now());
                    newProduct.setUnitCost(itemDTO.getUnitPrice());
                    newProduct = productRepository.save(newProduct);

                    item.setProductId(newProduct.getId());
                    item.setProductName(newProduct.getName());
                    item.setProductCode(newProduct.getCode());
                    item.setIsNewProduct(true);
                } else {
                    // Use existing product
                    Optional<Product> productOpt = productRepository.findById(itemDTO.getProductId());
                    if (productOpt.isPresent()) {
                        Product product = productOpt.get();
                        item.setProductId(product.getId());
                        item.setProductName(product.getName());
                        item.setProductCode(product.getCode());
                        item.setIsNewProduct(false);
                    }
                }

                item.setQuantity(itemDTO.getQuantity());
                item.setUnitPrice(itemDTO.getUnitPrice());
                item.setTotalPrice(itemDTO.getTotalPrice());
                item.setStatus("ORDERED");

                purchaseOrder.getItems().add(item);
                totalAmount += itemDTO.getTotalPrice();
            }
        }

        purchaseOrder.setTotalAmount(totalAmount);

        PurchaseOrder savedOrder = purchaseOrderRepository.save(purchaseOrder);

        // New Procurement Purchase -> configured reviewer(s)
        try {
            emailService.sendProcurementPurchaseCreated(savedOrder);
        } catch (Exception emailError) {
            // Never fail the purchase because notification mail failed.
            System.err.println("Failed to send Procurement Purchase reviewer email: " + emailError.getMessage());
        }

        return savedOrder;
    }

    @Transactional
    public PurchaseOrder updateOrderStatus(Long orderId, String status, String notes) {
        PurchaseOrder order = purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Purchase order not found"));

        order.setStatus(status);
        if (notes != null) {
            order.setNotes(order.getNotes() != null ? order.getNotes() + "\n" + notes : notes);
        }

        if ("DELIVERED".equals(status) || "RECEIVED".equals(status)) {
            // Automatically notify store when delivered
            if (!Boolean.TRUE.equals(order.getStoreNotified())) {
                order.setStoreNotified(true);
                order.setStoreNotificationDate(LocalDateTime.now());
            }
        }

        return purchaseOrderRepository.save(order);
    }

    @Transactional
    public PurchaseOrder notifyStore(Long orderId, String notificationNotes) {
        PurchaseOrder order = purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Purchase order not found"));

        order.setStoreNotified(true);
        order.setStoreNotificationDate(LocalDateTime.now());
        order.setNotes(order.getNotes() != null ?
                order.getNotes() + "\nStore notified: " + notificationNotes :
                "Store notified: " + notificationNotes);

        // Mark items as DELIVERED so they appear in store view
        for (PurchaseOrderItem item : order.getItems()) {
            if (!"STORE_RECEIVED".equals(item.getStatus())) {
                // Only mark as DELIVERED if not already received by store
                if (!"RECEIVED".equals(item.getStatus()) && !"STORE_RECEIVED".equals(item.getStatus())) {
                    item.setStatus("DELIVERED");
                }
            }
        }

        PurchaseOrder savedOrder = purchaseOrderRepository.save(order);

        // Explicit Notify Store action -> configured Store recipient(s)
        try {
            emailService.sendProcurementPurchaseStoreNotification(savedOrder, notificationNotes);
        } catch (Exception emailError) {
            // Do not roll back the business action if the SMTP server is unavailable.
            System.err.println("Failed to send Procurement Purchase Store email: " + emailError.getMessage());
        }

        return savedOrder;
    }

    @Transactional
    public PurchaseOrder receiveItems(Long orderId, Long itemId, Integer receivedQuantity, String receivedBy) {
        PurchaseOrder order = purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Purchase order not found"));

        PurchaseOrderItem item = order.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item not found"));

        if (receivedQuantity > item.getQuantity()) {
            throw new RuntimeException("Received quantity cannot exceed ordered quantity");
        }

        item.setReceivedQuantity(receivedQuantity);
        item.setStatus("PROCUREMENT_RECEIVED");

        // Check if all items are received
        boolean allReceived = order.getItems().stream()
                .allMatch(i -> "RECEIVED".equals(i.getStatus()));

        if (allReceived) {
            order.setStatus("RECEIVED");
        }

        order.setNotes(order.getNotes() != null ?
                order.getNotes() + "\nReceived " + receivedQuantity + " of " + item.getProductName() + " by " + receivedBy :
                "Received " + receivedQuantity + " of " + item.getProductName() + " by " + receivedBy);

        return purchaseOrderRepository.save(order);
    }

    public List<PurchaseOrder> getPurchaseOrdersByStatus(String status) {
        if (status == null || status.equals("all")) {
            return purchaseOrderRepository.findAll();
        }
        return purchaseOrderRepository.findByStatus(status.toUpperCase());
    }

    public List<PurchaseOrder> getOrdersPendingStoreNotification() {
        return purchaseOrderRepository.findPendingStoreNotifications();
    }

    public Optional<PurchaseOrder> getPurchaseOrderById(Long id) {
        return purchaseOrderRepository.findById(id);
    }

    public Optional<PurchaseOrder> getPurchaseOrderByNumber(String orderNumber) {
        return purchaseOrderRepository.findByPurchaseOrderNumber(orderNumber);
    }

    public List<PurchaseOrder> getOrdersForStore() {
        // Return all orders that are store notified
        List<PurchaseOrder> orders = purchaseOrderRepository.findByStoreNotifiedTrue();

        // Also include orders that are marked as delivered/received even if not notified
        // This ensures store sees all pending items
        List<PurchaseOrder> pendingOrders = purchaseOrderRepository.findByStatusIn(
                Arrays.asList("DELIVERED", "RECEIVED", "PROCUREMENT_RECEIVED")
        );

        // Merge and remove duplicates
        Set<PurchaseOrder> allOrders = new HashSet<>(orders);
        allOrders.addAll(pendingOrders);

        return new ArrayList<>(allOrders);
    }

    public List<PurchaseOrder> searchOrders(String searchTerm) {
        return purchaseOrderRepository.findBySupplierNameContainingIgnoreCase(searchTerm);
    }

    public List<PurchaseOrder> getUpcomingDeliveries() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(7); // Next 7 days
        return purchaseOrderRepository.findByExpectedDeliveryDateBetween(start, end);
    }

    @Transactional
    public StoreReceipt storeReceiveItems(StoreReceiptDTO dto) {
        PurchaseOrder order = purchaseOrderRepository.findById(dto.getPurchaseOrderId())
                .orElseThrow(() -> new RuntimeException("Purchase order not found"));

        PurchaseOrderItem item = order.getItems().stream()
                .filter(i -> i.getId().equals(dto.getItemId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item not found"));

        StoreReceipt receipt = new StoreReceipt();
        receipt.setPurchaseOrder(order);
        receipt.setItem(item);
        receipt.setGoodQuantity(dto.getGoodQuantity());
        receipt.setDamagedQuantity(dto.getDamagedQuantity());
        receipt.setReceivedBy(dto.getReceivedBy());
        receipt.setNotes(dto.getNotes());
        receipt.setReceivedDate(LocalDateTime.now());

        // Only update product stock for good items
        if (dto.getGoodQuantity() > 0) {
            Optional<Product> productOpt = productRepository.findById(item.getProductId());
            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                product.setStock(product.getStock() + dto.getGoodQuantity());
                productRepository.save(product);
                receipt.setStockUpdated(true);
            }
        }

        // Mark item as fully received by store
        if (item.getReceivedQuantity() <= (dto.getGoodQuantity() + dto.getDamagedQuantity())) {
            item.setStatus("STORE_RECEIVED");
        }

        return storeReceiptRepository.save(receipt);
    }
}