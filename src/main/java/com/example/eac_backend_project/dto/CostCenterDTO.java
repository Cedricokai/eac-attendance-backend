package com.example.eac_backend_project.dto;

import com.example.eac_backend_project.entity.inventory.CostCenterStatus;
import com.example.eac_backend_project.entity.inventory.CostCenterType;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostCenterDTO {
    private Long id;

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

    private Double spentAmount = 0.0;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private Long managerId;
    private String managerName;

    private List<CostCenterProductDTO> products;
    private List<JobDTO> jobs;

    private Double budgetUtilization;
    private Double remainingBudget;
    private Boolean isCriticalBudget;
    private Boolean isWarningBudget;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}