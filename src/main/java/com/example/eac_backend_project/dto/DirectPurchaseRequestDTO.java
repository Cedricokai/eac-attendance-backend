package com.example.eac_backend_project.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class DirectPurchaseRequestDTO {
    private LocalDate requestDate;
    private String requester;
    private String accountant;
    private String notes;
    private List<DirectPurchaseItemDTO> items;
}