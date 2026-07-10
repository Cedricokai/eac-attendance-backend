package com.example.eac_backend_project.dto;

import com.example.eac_backend_project.entity.inventory.CostCenterProductStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostCenterProductDTO {
    private Long id;

    @NotNull(message = "Cost center ID is required")
    private Long costCenterId;

    @NotNull(message = "Product ID is required")
    private Long productId;

    private String productName;
    private String productCode;
    private String productType;
    private String brand;
    private Integer availableStock;
    private Double unitCost;

    @NotNull(message = "Requested quantity is required")
    @Min(value = 1, message = "Requested quantity must be at least 1")
    private Integer requestedQuantity;

    private Integer approvedQuantity;
    private Integer issuedQuantity;

    private String estimatedUsage;

    @NotNull(message = "Status is required")
    private CostCenterProductStatus status;

    private Double totalCost;
    private String remarks;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime requestDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approvalDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime issueDate;
}