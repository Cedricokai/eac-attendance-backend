package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.dto.PurchaseOrderDTO;
import com.example.eac_backend_project.entity.inventory.PurchaseOrder;
import com.example.eac_backend_project.Service.PurchaseOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/purchase-orders")
@CrossOrigin(origins = "*")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    @PostMapping
    public ResponseEntity<?> createPurchaseOrder(@RequestBody PurchaseOrderDTO dto, Authentication authentication) {
        try {
            String username = authentication.getName();
            PurchaseOrder order = purchaseOrderService.createPurchaseOrder(dto, username);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<PurchaseOrder>> getAllPurchaseOrders(@RequestParam(required = false) String status) {
        List<PurchaseOrder> orders = purchaseOrderService.getPurchaseOrdersByStatus(status);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/pending-notifications")
    public ResponseEntity<List<PurchaseOrder>> getPendingNotifications() {
        List<PurchaseOrder> orders = purchaseOrderService.getOrdersPendingStoreNotification();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/for-store")
    public ResponseEntity<List<PurchaseOrder>> getOrdersForStore() {
        List<PurchaseOrder> orders = purchaseOrderService.getOrdersForStore();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<PurchaseOrder>> getUpcomingDeliveries() {
        List<PurchaseOrder> orders = purchaseOrderService.getUpcomingDeliveries();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPurchaseOrderById(@PathVariable Long id) {
        try {
            PurchaseOrder order = purchaseOrderService.getPurchaseOrderById(id)
                    .orElseThrow(() -> new RuntimeException("Purchase order not found"));
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String status = request.get("status");
            String notes = request.get("notes");
            PurchaseOrder order = purchaseOrderService.updateOrderStatus(id, status, notes);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/notify-store")
    public ResponseEntity<?> notifyStore(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String notes = request.get("notes");
            PurchaseOrder order = purchaseOrderService.notifyStore(id, notes);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{orderId}/receive/{itemId}")
    public ResponseEntity<?> receiveItems(@PathVariable Long orderId,
                                          @PathVariable Long itemId,
                                          @RequestBody Map<String, Object> request) {
        try {
            Integer receivedQuantity = (Integer) request.get("receivedQuantity");
            String receivedBy = (String) request.get("receivedBy");
            PurchaseOrder order = purchaseOrderService.receiveItems(orderId, itemId, receivedQuantity, receivedBy);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<PurchaseOrder>> searchOrders(@RequestParam String query) {
        List<PurchaseOrder> orders = purchaseOrderService.searchOrders(query);
        return ResponseEntity.ok(orders);
    }
}