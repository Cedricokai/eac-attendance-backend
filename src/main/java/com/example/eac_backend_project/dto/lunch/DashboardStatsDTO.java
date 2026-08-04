package com.example.eac_backend_project.dto.lunch;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DashboardStatsDTO {
    private long todayPresent;
    private long todayAssigned;
    private long todayServed;
    private long pendingLunch;
    private long absentEmployees;
    private long notAssignedEmployees;
    private long weeklyAssigned;
    private long monthlyServed;
    private BigDecimal todayEstimatedCost;
    private BigDecimal monthlyFoodCost;
    private BigDecimal todayBudget;
}