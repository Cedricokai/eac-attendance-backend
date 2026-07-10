package com.example.eac_backend_project.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DriverDTO {
    private Long id;
    private String driverName;
    private String licenseNumber;
    private LocalDate licenseExpiry;
    private String contact;
    private String status;
}