package com.example.eac_backend_project.dto;

import lombok.Data;
import java.util.Map;

@Data
public class RuleSetDTO {
    private Long id;
    private String name;
    private String description;
    private Map<String, RuleDTO> rules;

    @Data
    public static class RuleDTO {
        private Integer expectedHours;
        private String description;
    }
}