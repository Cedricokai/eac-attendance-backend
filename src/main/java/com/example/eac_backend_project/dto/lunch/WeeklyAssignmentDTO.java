package com.example.eac_backend_project.dto.lunch;

import lombok.Data;

import java.time.LocalDate;
import java.util.Map;

@Data
public class WeeklyAssignmentDTO {
    private Long employeeId;
    private LocalDate startDate;      // Monday of the week
    private Map<String, Long> days;   // key: "MONDAY", value: mealId (null allowed)
}