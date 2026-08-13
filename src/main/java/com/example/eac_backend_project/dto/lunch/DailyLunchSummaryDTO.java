package com.example.eac_backend_project.dto.lunch;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DailyLunchSummaryDTO {
    private LocalDate date;
    private long presentEmployees;
    private long assignedEmployees;
    private long servedEmployees;
    private long pendingLunch;
    private long absentEmployees;
    private long notAssignedEmployees;
    // additional fields as needed
}