package com.example.eac_backend_project.dto;

import lombok.Data;

@Data
public class StockAdjustmentRequest {
    private Long productId;
    private String productName;
    private Integer oldStock;
    private Integer newStock;
    private Integer quantity;
    private String adjustmentType; // "ADD" or "REMOVE"
    private String reason;
    private String notes;
    private String userEmail;
}