package com.example.eac_backend_project.dto;

import com.example.eac_backend_project.entity.directpurchase.DirectPurchaseStatus;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class DirectPurchaseResponseDTO {
    private Long id;
    private String requestNumber;
    private LocalDate requestDate;
    private String requester;
    private String accountant;
    private String notes;
    private DirectPurchaseStatus status;
    private Double totalAmount;
    private List<DirectPurchaseItemDTO> items;
    private String currentStepName;
    private String currentRequiredRole;
}