package com.example.eac_backend_project.dto.lunch;

import lombok.Data;

import java.time.LocalDate;

@Data
public class LunchAssignmentDTO {
    private Long employeeId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private String reason;
}