package com.example.eac_backend_project.dto;

import com.example.eac_backend_project.entity.attendance.TimesheetStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class TimesheetListDTO {

    private Long id;
    private LocalDate date;
    private String attendanceCode;

    private Double regularHours;
    private Double overtimeHours;
    private Double breakHours;
    private Double totalHours;
    private Double earnings;

    private TimesheetStatus status;
    private String notes;

    private Boolean sourceDocumentMissing;
    private String sourceDocumentMissingReason;
    private LocalDateTime sourceDocumentMissingFlaggedAt;
    private String sourceDocumentMissingFlaggedBy;

    private String sourceDocumentType;
    private String sourceDocumentReference;
    private LocalDateTime verifiedAt;
    private String verifiedBy;

    private Long employeeId;
    private String employeeNumber;
    private String firstName;
    private String lastName;

    private Long categoryId;
    private String categoryName;

    private Long jobId;
    private String jobName;

    private Long overviewId;

    public TimesheetListDTO(
            Long id,
            LocalDate date,
            String attendanceCode,
            Double regularHours,
            Double overtimeHours,
            Double breakHours,
            Double totalHours,
            Double earnings,
            TimesheetStatus status,
            String notes,
            Boolean sourceDocumentMissing,
            String sourceDocumentMissingReason,
            LocalDateTime sourceDocumentMissingFlaggedAt,
            String sourceDocumentMissingFlaggedBy,
            String sourceDocumentType,
            String sourceDocumentReference,
            LocalDateTime verifiedAt,
            String verifiedBy,
            Long employeeId,
            String employeeNumber,
            String firstName,
            String lastName,
            Long categoryId,
            String categoryName,
            Long jobId,
            String jobName,
            Long overviewId
    ) {
        this.id = id;
        this.date = date;
        this.attendanceCode = attendanceCode;
        this.regularHours = regularHours;
        this.overtimeHours = overtimeHours;
        this.breakHours = breakHours;
        this.totalHours = totalHours;
        this.earnings = earnings;
        this.status = status;
        this.notes = notes;
        this.sourceDocumentMissing = sourceDocumentMissing;
        this.sourceDocumentMissingReason = sourceDocumentMissingReason;
        this.sourceDocumentMissingFlaggedAt =
                sourceDocumentMissingFlaggedAt;
        this.sourceDocumentMissingFlaggedBy =
                sourceDocumentMissingFlaggedBy;
        this.sourceDocumentType = sourceDocumentType;
        this.sourceDocumentReference = sourceDocumentReference;
        this.verifiedAt = verifiedAt;
        this.verifiedBy = verifiedBy;
        this.employeeId = employeeId;
        this.employeeNumber = employeeNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.jobId = jobId;
        this.jobName = jobName;
        this.overviewId = overviewId;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getAttendanceCode() {
        return attendanceCode;
    }

    public Double getRegularHours() {
        return regularHours;
    }

    public Double getOvertimeHours() {
        return overtimeHours;
    }

    public Double getBreakHours() {
        return breakHours;
    }

    public Double getTotalHours() {
        return totalHours;
    }

    public Double getEarnings() {
        return earnings;
    }

    public TimesheetStatus getStatus() {
        return status;
    }

    public String getNotes() {
        return notes;
    }

    public Boolean getSourceDocumentMissing() {
        return sourceDocumentMissing;
    }

    public String getSourceDocumentMissingReason() {
        return sourceDocumentMissingReason;
    }

    public LocalDateTime getSourceDocumentMissingFlaggedAt() {
        return sourceDocumentMissingFlaggedAt;
    }

    public String getSourceDocumentMissingFlaggedBy() {
        return sourceDocumentMissingFlaggedBy;
    }

    public String getSourceDocumentType() {
        return sourceDocumentType;
    }

    public String getSourceDocumentReference() {
        return sourceDocumentReference;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public String getVerifiedBy() {
        return verifiedBy;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public String getEmployeeNumber() {
        return employeeNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public Long getJobId() {
        return jobId;
    }

    public String getJobName() {
        return jobName;
    }

    public Long getOverviewId() {
        return overviewId;
    }
}