package com.example.eac_backend_project.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public class AttendanceListDTO {

    private Long id;
    private LocalDate date;
    private LocalTime checkIn;
    private LocalTime checkOut;
    private Double minimumHour;
    private Double overtime;
    private Double standardHours;
    private String shift;
    private String workType;
    private String status;
    private Boolean biometric;

    private Long employeeId;
    private String employeeNumber;
    private String firstName;
    private String lastName;

    private Long categoryId;
    private String categoryName;

    public AttendanceListDTO(
            Long id,
            LocalDate date,
            LocalTime checkIn,
            LocalTime checkOut,
            Double minimumHour,
            Double overtime,
            Double standardHours,
            String shift,
            String workType,
            String status,
            Boolean biometric,
            Long employeeId,
            String employeeNumber,
            String firstName,
            String lastName,
            Long categoryId,
            String categoryName
    ) {
        this.id = id;
        this.date = date;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.minimumHour = minimumHour;
        this.overtime = overtime;
        this.standardHours = standardHours;
        this.shift = shift;
        this.workType = workType;
        this.status = status;
        this.biometric = biometric;
        this.employeeId = employeeId;
        this.employeeNumber = employeeNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getCheckIn() {
        return checkIn;
    }

    public LocalTime getCheckOut() {
        return checkOut;
    }

    public Double getMinimumHour() {
        return minimumHour;
    }

    public Double getOvertime() {
        return overtime;
    }

    public Double getStandardHours() {
        return standardHours;
    }

    public String getShift() {
        return shift;
    }

    public String getWorkType() {
        return workType;
    }

    public String getStatus() {
        return status;
    }

    public Boolean getBiometric() {
        return biometric;
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
}