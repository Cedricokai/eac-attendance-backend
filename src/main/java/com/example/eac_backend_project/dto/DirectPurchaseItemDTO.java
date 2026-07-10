package com.example.eac_backend_project.dto;

import lombok.Data;

@Data
public class DirectPurchaseItemDTO {
    private Long id;
    private String description;
    private Integer quantity;
    private Double unitPrice;
    private Double total;
}