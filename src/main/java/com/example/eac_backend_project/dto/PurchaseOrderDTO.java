package com.example.eac_backend_project.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PurchaseOrderDTO {
    private String purchaseOrderNumber;
    private String supplierName;
    private String supplierContact;
    private LocalDateTime orderDate;
    private LocalDateTime expectedDeliveryDate;
    private String deliveryAddress;
    private Double totalAmount;
    private String status; // PENDING, ORDERED, DELIVERED, RECEIVED
    private String notes;
    private String orderedBy;
    private List<PurchaseItemDTO> items;

    @Data
    public static class PurchaseItemDTO {
        private Long productId;
        private String productName;
        private String productCode;
        private Integer quantity;
        private Double unitPrice;
        private Double totalPrice;
        private Boolean isNewProduct; // If this is a new product not in inventory
    }
}