package com.example.eac_backend_project.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public class AttendanceReportEmailRequest {

    @NotEmpty
    private List<String> toEmails;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private String subject;
    private String message;
    private Long jobId;
    private List<Long> employeeIds;
    private List<Long> excludeOvertimeEmployeeIds;
    private Boolean includeOvertime = true;

    public AttendanceReportEmailRequest() {}

    public List<String> getToEmails() {
        return toEmails;
    }

    public void setToEmails(List<String> toEmails) {
        this.toEmails = toEmails;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public List<Long> getEmployeeIds() { return employeeIds; }
    public void setEmployeeIds(List<Long> employeeIds) { this.employeeIds = employeeIds; }
    public List<Long> getExcludeOvertimeEmployeeIds() { return excludeOvertimeEmployeeIds; }
    public void setExcludeOvertimeEmployeeIds(List<Long> excludeOvertimeEmployeeIds) { this.excludeOvertimeEmployeeIds = excludeOvertimeEmployeeIds; }
    public Boolean getIncludeOvertime() { return includeOvertime; }
    public void setIncludeOvertime(Boolean includeOvertime) { this.includeOvertime = includeOvertime; }
}
