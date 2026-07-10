package com.example.eac_backend_project.dto;


import com.example.eac_backend_project.entity.inventory.CostCenterProductStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAssignmentDTO {

    @NotNull(message = "Cost center ID is required")
    private Long costCenterId;

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Requested quantity is required")
    @Min(value = 1, message = "Requested quantity must be at least 1")
    private Integer requestedQuantity;

    private String estimatedUsage;

    private CostCenterProductStatus status;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class BulkProductAssignmentDTO {

    @NotNull(message = "Cost center ID is required")
    private Long costCenterId;

    @NotNull(message = "Products list cannot be null")
    private List<ProductAssignmentDTO> products;
}
