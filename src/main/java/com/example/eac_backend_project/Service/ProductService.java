package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.dto.StockAdjustmentRequest;
import com.example.eac_backend_project.entity.inventory.*;
import com.example.eac_backend_project.repository.OutgoingRepository;
import com.example.eac_backend_project.repository.ProductHistoryRepository;
import com.example.eac_backend_project.repository.ProductRepository;
import com.example.eac_backend_project.repository.StockHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final OutgoingRepository outgoingRepository;
    private final ProductHistoryRepository productHistoryRepository;
    private final StockHistoryRepository stockHistoryRepository;
    private final Logger logger = LoggerFactory.getLogger(ProductService.class);

    public ProductService(ProductRepository productRepository, OutgoingRepository outgoingRepository, ProductHistoryRepository productHistoryRepository, StockHistoryRepository stockHistoryRepository) {
        this.productRepository = productRepository;
        this.outgoingRepository = outgoingRepository;
        this.productHistoryRepository = productHistoryRepository;
        this.stockHistoryRepository = stockHistoryRepository;
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product addProduct(Product product, String userEmail) {
        Product savedProduct = productRepository.save(product);
        logProductHistory(savedProduct, ActionType.ADD, userEmail);
        logger.info("Product added: {}", savedProduct.getName());
        return savedProduct;
    }

    public Product updateProduct(Long id, Product updatedProduct, String userEmail) {
        Optional<Product> existingProductOptional = productRepository.findById(id);
        if (existingProductOptional.isPresent()) {
            Product existingProduct = existingProductOptional.get();

            existingProduct.setName(updatedProduct.getName());
            existingProduct.setCode(updatedProduct.getCode());
            existingProduct.setDescription(updatedProduct.getDescription());
            existingProduct.setStock(updatedProduct.getStock()); // Frontend sends updated stock
            existingProduct.setUserName(updatedProduct.getUserName());
            existingProduct.setUnitCost(updatedProduct.getUnitCost());
            existingProduct.setProductType(updatedProduct.getProductType());
            existingProduct.setEntryDate(updatedProduct.getEntryDate());
            existingProduct.setPpe(updatedProduct.getPpe());
          existingProduct.setKitchenStore(updatedProduct.getKitchenStore());
          existingProduct.setReturnableAfterUse(updatedProduct.getReturnableAfterUse());
          existingProduct.setShelfNumber(updatedProduct.getShelfNumber());

            Product savedProduct = productRepository.save(existingProduct);
            logProductHistory(savedProduct, ActionType.EDIT, userEmail);
            return savedProduct;
        } else {
            throw new RuntimeException("Product with ID " + id + " not found.");
        }
    }

    public boolean existsById(Long id) {
        return productRepository.existsById(id);
    }

    public void deleteProduct(Long id, String userEmail) {
        Optional<Product> productOptional = productRepository.findById(id);
        if (productOptional.isPresent()) {
            Product product = productOptional.get();
            logProductHistory(product, ActionType.DELETE, userEmail);
            productRepository.deleteById(id);
        } else {
            throw new RuntimeException("Product with ID " + id + " not found.");
        }
    }

    public List<Product> searchProducts(String query) {
        return productRepository.findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(query, query);
    }

    // ✅ REMOVED: moveProductsToOutgoing method with stock deduction
    // Frontend will handle stock deduction and send updated products

    @Transactional
    public void saveOutgoingRecords(List<Outgoing> outgoingItems) {
        // Just save outgoing records, no stock deduction
        for (Outgoing outgoing : outgoingItems) {
            if (outgoing.getProductId() == null) {
                throw new IllegalArgumentException("Product ID must not be null for outgoing item: " + outgoing.getName());
            }
            if (outgoing.getMovementDate() == null) {
                outgoing.setMovementDate(LocalDateTime.now());
            }
        }
        outgoingRepository.saveAll(outgoingItems);
        logger.info("Saved {} outgoing records", outgoingItems.size());
    }

    private void logProductHistory(Product product, ActionType actionType, String userEmail) {
        ProductHistory history = new ProductHistory();
        history.setProductId(product.getId());
        history.setActionType(actionType);
        history.setActionDate(LocalDateTime.now());
        history.setUserEmail(userEmail);
        history.setDescription("The Action '" + actionType.name() + "' was performed on the product '" + product.getName() + "'");
        productHistoryRepository.save(history);
    }

    @Transactional
    public Product updateProductStock(Long productId, int newStock, String userEmail) {
        Optional<Product> productOptional = productRepository.findById(productId);
        if (productOptional.isPresent()) {
            Product product = productOptional.get();
            int oldStock = product.getStock();
            product.setStock(newStock);
            productRepository.save(product);
            logProductHistory(product, ActionType.EDIT, userEmail);
            logger.info("Stock updated for product {} from {} to {}", product.getName(), oldStock, newStock);
            return product;
        } else {
            throw new RuntimeException("Product with ID " + productId + " not found.");
        }
    }

    public long getTotalNumberOfProducts() {
        return productRepository.count();
    }

    public int getTotalStock() {
        List<Product> products = productRepository.findAll();
        return products.stream()
                .mapToInt(Product::getStock)
                .sum();
    }

    @Transactional
    public Product adjustProductStock(StockAdjustmentRequest request) {
        Optional<Product> productOptional = productRepository.findById(request.getProductId());
        if (productOptional.isPresent()) {
            Product product = productOptional.get();

            // Verify old stock matches current stock
            int currentStock = product.getStock();
            if (!request.getOldStock().equals(currentStock)) {
                throw new RuntimeException("Stock has changed since you started this adjustment. Current stock: " + currentStock +
                        ", Your old stock: " + request.getOldStock());
            }

            // Update product stock
            product.setStock(request.getNewStock());
            Product savedProduct = productRepository.save(product);

            // Log detailed stock history
            logStockHistory(request, product.getName());

            // Log general product history
            logProductHistory(product, ActionType.EDIT, request.getUserEmail(),
                    "Stock adjusted from " + request.getOldStock() + " to " + request.getNewStock());

            logger.info("Stock adjusted for product {}: {} -> {} ({} {} units)",
                    product.getName(), request.getOldStock(), request.getNewStock(),
                    request.getQuantity(), request.getAdjustmentType().toLowerCase());

            return savedProduct;
        } else {
            throw new RuntimeException("Product with ID " + request.getProductId() + " not found.");
        }
    }

    private void logStockHistory(StockAdjustmentRequest request, String productName) {
        StockHistory stockHistory = new StockHistory();
        stockHistory.setProductId(request.getProductId());
        stockHistory.setProductName(productName);
        stockHistory.setOldStock(request.getOldStock());
        stockHistory.setNewStock(request.getNewStock());
        stockHistory.setQuantityChanged(request.getQuantity());
        stockHistory.setAdjustmentType(request.getAdjustmentType());
        stockHistory.setReason(request.getReason());
        stockHistory.setNotes(request.getNotes());
        stockHistory.setUserEmail(request.getUserEmail());
        stockHistory.setAdjustmentDate(LocalDateTime.now());

        stockHistoryRepository.save(stockHistory);
    }

    private void logProductHistory(Product product, ActionType actionType, String userEmail, String description) {
        ProductHistory history = new ProductHistory();
        history.setProductId(product.getId());
        history.setActionType(actionType);
        history.setActionDate(LocalDateTime.now());
        history.setUserEmail(userEmail);
        history.setDescription(description);
        productHistoryRepository.save(history);
    }

    public List<StockHistory> getStockHistoryByProductId(Long productId) {
        return stockHistoryRepository.findByProductIdOrderByAdjustmentDateDesc(productId);
    }

    public List<StockHistory> getAllStockHistory() {
        return stockHistoryRepository.findAll();
    }

}