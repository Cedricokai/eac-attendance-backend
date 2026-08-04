package com.example.eac_backend_project.dto.lunch;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyServingStatsDTO {
    private LocalDate date;
    private long totalEmployees;
    private long presentCount;
    private long assignedCount;
    private long servedCount;
    private long pendingCount;
    private long absentCount;
    private Map<String, Long> locationStats; // e.g., { "MKV": 10, "LAYDOWN": 5 }
    private Map<String, Long> mealStats;     // e.g., { "Jollof Rice": 15, "Banku": 10 }
}