package com.example.eac_backend_project.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateCostCenterDTO {
    private String name;
    private String code;
    private String description;
    private String type;
    private String status;
    private Double budget;
    private Long jobId;  // Send jobId instead of Job object
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}