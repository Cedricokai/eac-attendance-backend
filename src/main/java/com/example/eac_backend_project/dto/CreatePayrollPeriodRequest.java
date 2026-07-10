package com.example.eac_backend_project.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class CreatePayrollPeriodRequest {
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private String category; // "Permanent", "Contract", "Casual", etc.
    private boolean convertExcessToOvertime = true;

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}