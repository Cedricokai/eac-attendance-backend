package com.example.eac_backend_project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageDTO {
    private Long id;
    private String name;
    private String path;
    private String description;
    private Boolean isPublic;
    private String iconName;
    private Integer displayOrder;
    private String module;
    private Boolean isActive;
    private Boolean requiresAuth;
    private Set<String> roleNames;
    private Set<Long> userIds;

    // Full constructor
    public PageDTO(Long id, String name, String path, String description,
                   Boolean isPublic, String iconName, Integer displayOrder,
                   String module, Boolean isActive, Boolean requiresAuth,
                   Set<String> roleNames) {
        this.id = id;
        this.name = name;
        this.path = path;
        this.description = description;
        this.isPublic = isPublic;
        this.iconName = iconName;
        this.displayOrder = displayOrder;
        this.module = module;
        this.isActive = isActive;
        this.requiresAuth = requiresAuth;
        this.roleNames = roleNames;
    }

    // Simplified constructor for backward compatibility
    public PageDTO(Long id, String name, String path, String description,
                   Boolean isPublic, String iconName, Integer displayOrder,
                   Set<String> roleNames) {
        this(id, name, path, description, isPublic, iconName, displayOrder,
                "GENERAL", true, true, roleNames);
    }
}