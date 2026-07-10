package com.example.eac_backend_project.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class AttendanceRangeCreateRequest {

    private List<Long> employeeIds;
    private LocalDate startDate;
    private LocalDate endDate;

    private String status;     // default if null
    private String shift;      // default if null
    private String workType;   // default if null
    private LocalTime checkIn; // optional
    private LocalTime checkOut;// optional

    private boolean overwriteExisting;
}
