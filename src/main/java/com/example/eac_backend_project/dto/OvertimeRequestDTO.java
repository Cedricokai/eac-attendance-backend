package com.example.eac_backend_project.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class OvertimeRequestDTO {
    private Long employeeId;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;
    private Double baseHourlyRate;
    private Double overtimeMultiplier;
    private Double overtimeHours;
    private Double calculatedOvertimePay;
    private Boolean multiplierAppliedToHours; // Add this field
    private Double rawOvertimeHours; // ADD THIS

    // Getters and Setters
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Double getBaseHourlyRate() { return baseHourlyRate; }
    public void setBaseHourlyRate(Double baseHourlyRate) { this.baseHourlyRate = baseHourlyRate; }
    public Double getOvertimeMultiplier() { return overtimeMultiplier; }
    public void setOvertimeMultiplier(Double overtimeMultiplier) { this.overtimeMultiplier = overtimeMultiplier; }
    public Double getOvertimeHours() { return overtimeHours; }
    public void setOvertimeHours(Double overtimeHours) { this.overtimeHours = overtimeHours; }
    public Double getCalculatedOvertimePay() { return calculatedOvertimePay; }
    public void setCalculatedOvertimePay(Double calculatedOvertimePay) { this.calculatedOvertimePay = calculatedOvertimePay; }
}