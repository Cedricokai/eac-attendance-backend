package com.example.eac_backend_project.dto.lunch;

import lombok.Data;

import java.time.LocalDate;

@Data
public class LunchServingDTO {
    private Long employeeId;
    private LocalDate lunchDate;
    private String device;
    private String remarks;
    private String location;
}