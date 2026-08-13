package com.example.eac_backend_project.dto.lunch;

import lombok.Data;

import java.time.LocalDate;

@Data
public class KitchenReportDTO {
    private LocalDate date;
    private long totalRequired;
    private long assigned;
    private long served;
    private long remaining;
}