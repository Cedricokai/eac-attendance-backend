package com.example.eac_backend_project.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TimesheetDTO {
    private Long employeeId;
    private LocalDate date;
    private Double regularHours;
    private Double overtimeHours;
    private Double breakHours;
    private Double totalHours;
    private Double earnings;
    private String status;
    private String notes;
    private String attendanceCode;
    private Boolean sourceDocumentMissing;
    private String sourceDocumentMissingReason;
    private String sourceDocumentType;
    private String sourceDocumentReference;
    private LocalDateTime verifiedAt;
    private String verifiedBy;

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Double getRegularHours() {
        return regularHours;
    }

    public void setRegularHours(Double regularHours) {
        this.regularHours = regularHours;
    }

    public Double getOvertimeHours() {
        return overtimeHours;
    }

    public void setOvertimeHours(Double overtimeHours) {
        this.overtimeHours = overtimeHours;
    }

    public Double getBreakHours() {
        return breakHours;
    }

    public void setBreakHours(Double breakHours) {
        this.breakHours = breakHours;
    }

    public Double getTotalHours() {
        return totalHours;
    }

    public void setTotalHours(Double totalHours) {
        this.totalHours = totalHours;
    }

    public Double getEarnings() {
        return earnings;
    }

    public void setEarnings(Double earnings) {
        this.earnings = earnings;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}