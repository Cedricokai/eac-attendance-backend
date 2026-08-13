package com.example.eac_backend_project.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class DirectPurchaseRequestDTO {

    private LocalDate requestDate;

    /*
     * Requester is resolved from the authenticated user.
     * These fields are retained for backward compatibility only.
     */
    private String requester;
    private String requesterEmail;

    @NotNull(message = "Accountant user is required")
    private Long accountantUserId;

    private String accountant;
    private String accountantEmail;

    private String notes;

    @Valid
    @NotEmpty(message = "At least one item is required")
    private List<DirectPurchaseItemDTO> items;
}
