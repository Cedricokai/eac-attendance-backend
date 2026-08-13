package com.example.eac_backend_project.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public class OvertimeListDTO {

    private Long id;

    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;

    private Double overtimeHours;
    private Double rawOvertimeHours;

    private String status;
    private String notes;
    private String overtimeCategory;

    private Double baseHourlyRate;
    private Double overtimeMultiplier;
    private Double calculatedOvertimePay;
    private Boolean multiplierAppliedToHours;

    private Long attendanceId;

    private Long employeeId;
    private String employeeNumber;
    private String firstName;
    private String lastName;
    private Double employeeMinimumRate;

    private Long categoryId;
    private String categoryName;

    public OvertimeListDTO(
            Long id,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            Double overtimeHours,
            Double rawOvertimeHours,
            String status,
            String notes,
            String overtimeCategory,
            Double baseHourlyRate,
            Double overtimeMultiplier,
            Double calculatedOvertimePay,
            Boolean multiplierAppliedToHours,
            Long attendanceId,
            Long employeeId,
            String employeeNumber,
            String firstName,
            String lastName,
            Double employeeMinimumRate,
            Long categoryId,
            String categoryName
    ) {
        this.id = id;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.overtimeHours = overtimeHours;
        this.rawOvertimeHours = rawOvertimeHours;
        this.status = status;
        this.notes = notes;
        this.overtimeCategory = overtimeCategory;
        this.baseHourlyRate = baseHourlyRate;
        this.overtimeMultiplier = overtimeMultiplier;
        this.calculatedOvertimePay = calculatedOvertimePay;
        this.multiplierAppliedToHours = multiplierAppliedToHours;
        this.attendanceId = attendanceId;
        this.employeeId = employeeId;
        this.employeeNumber = employeeNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.employeeMinimumRate = employeeMinimumRate;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public Double getOvertimeHours() {
        return overtimeHours;
    }

    public Double getRawOvertimeHours() {
        return rawOvertimeHours;
    }

    public String getStatus() {
        return status;
    }

    public String getNotes() {
        return notes;
    }

    public String getOvertimeCategory() {
        return overtimeCategory;
    }

    public Double getBaseHourlyRate() {
        return baseHourlyRate;
    }

    public Double getOvertimeMultiplier() {
        return overtimeMultiplier;
    }

    public Double getCalculatedOvertimePay() {
        return calculatedOvertimePay;
    }

    public Boolean getMultiplierAppliedToHours() {
        return multiplierAppliedToHours;
    }

    public Long getAttendanceId() {
        return attendanceId;
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

    public Double getEmployeeMinimumRate() {
        return employeeMinimumRate;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }
}