package com.example.eac_backend_project.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class AttendanceBatchDTO {
    private Long employeeId;
    private LocalDate date;
    private String status;
    private String shift;
    private String workType;
    private String checkIn;   // string from frontend
    private String checkOut;  // string from frontend
}
