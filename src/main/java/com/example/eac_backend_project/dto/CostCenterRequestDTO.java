package com.example.eac_backend_project.dto;

import com.example.eac_backend_project.entity.inventory.CostCenterStatus;
import com.example.eac_backend_project.entity.inventory.CostCenterType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostCenterRequestDTO {

    @NotBlank(message = "Code is required")
    @Size(min = 2, max = 50, message = "Code must be between 2 and 50 characters")
    private String code;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 200, message = "Name must be between 2 and 200 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Type is required")
    private CostCenterType type;

    @NotNull(message = "Status is required")
    private CostCenterStatus status;

    @NotNull(message = "Budget is required")
    @Positive(message = "Budget must be positive")
    private Double budget;

    private LocalDate startDate;
    private LocalDate endDate;

    private Long managerId;
}