package com.example.eac_backend_project.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** Scalar list projection: attachments are downloaded only through the attachment endpoint. */
public record LeaveListDTO(
    Long id,
    LocalDate startDate,
    LocalDate endDate,
    String leaveType,
    String reason,
    String status,
    String supervisorStatus,
    String supervisorFeedback,
    LocalDateTime supervisorActionDate,
    String plannerStatus,
    String plannerFeedback,
    LocalDateTime plannerActionDate,
    String hrStatus,
    String hrFeedback,
    LocalDateTime hrActionDate,
    LocalDateTime createdAt,
    LocalDateTime submittedDate,
    String attachmentFileName,
    String attachmentFileType,
    Long attachmentFileSize,
    Double totalLeaveHours,
    String weekendCalculationMethod,
    @JsonIgnore Long employeeId,
    @JsonIgnore String employeeNumber,
    @JsonIgnore String firstName,
    @JsonIgnore String lastName,
    @JsonIgnore String department,
    @JsonIgnore String email,
    @JsonIgnore Long categoryId,
    @JsonIgnore String categoryName,
    @JsonIgnore Boolean excludeWeekendsFromLeave,
    @JsonIgnore Integer annualLeaveDays,
    @JsonIgnore Integer standardRateHours
) {
    public EmployeeSummary getEmployee() {
        return employeeId == null ? null : new EmployeeSummary(employeeId, employeeNumber, firstName, lastName,
            department, email, categoryId == null ? null : new CategorySummary(categoryId, categoryName,
                excludeWeekendsFromLeave, annualLeaveDays, standardRateHours));
    }
    public record EmployeeSummary(Long id, String employeeId, String firstName, String lastName,
        String department, String email, CategorySummary category) {}
    public record CategorySummary(Long id, String name, Boolean excludeWeekendsFromLeave,
        Integer annualLeaveDays, Integer standardRateHours) {}
}
