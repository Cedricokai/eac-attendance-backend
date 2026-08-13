package com.example.eac_backend_project.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record AttendanceRequestResponseDTO(
        Long id,
        Long employeeId,
        String employeeName,
        EmployeeSummary employee,
        LocalDate date,
        @JsonFormat(pattern = "HH:mm:ss") LocalTime checkIn,
        @JsonFormat(pattern = "HH:mm:ss") LocalTime checkOut,
        String shift,
        String reason,
        String evidenceReference,
        String status,
        String requestedBy,
        LocalDateTime requestedAt,
        String reviewedBy,
        LocalDateTime reviewedAt,
        String rejectionReason,
        Long attendanceId
) {
    public record EmployeeSummary(Long id, String employeeId, String firstName, String lastName) {}
}
