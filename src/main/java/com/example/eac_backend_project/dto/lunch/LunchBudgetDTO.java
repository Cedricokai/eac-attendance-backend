package com.example.eac_backend_project.dto.lunch;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class LunchBudgetDTO {
    private String budgetType;
    private BigDecimal amount;
    private LocalDate effectiveDate;
    private Long departmentId;
}