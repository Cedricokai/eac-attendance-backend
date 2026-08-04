package com.example.eac_backend_project.dto.lunch;

import lombok.Data;

@Data
public class LunchSettingsDTO {
    private String settingKey;
    private String settingValue;
    private String description;
}