package com.example.eac_backend_project.dto;

import lombok.Data;

@Data
public class ToggleActiveStatusDTO {
    private Boolean active;
    private String reason;
    private String processedBy;
}