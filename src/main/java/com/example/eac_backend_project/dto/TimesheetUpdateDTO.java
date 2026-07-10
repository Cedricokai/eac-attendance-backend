package com.example.eac_backend_project.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class TimesheetUpdateDTO {
    private LocalDate date;
    private String attendanceCode;
    private Double regularHours;
    private Double overtimeHours;
    private Double breakHours;
    private Double earnings;
    private String notes;
    private Boolean sourceDocumentMissing;
    private String sourceDocumentMissingReason;
    private String sourceDocumentMissingFlaggedBy;
    private String sourceDocumentType;
    private String sourceDocumentReference;
}
