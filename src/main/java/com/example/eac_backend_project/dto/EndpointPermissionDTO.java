package com.example.eac_backend_project.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EndpointPermissionDTO {
    private Long id;
    private String httpMethod;
    private String endpointPattern;
    private String description;
    private Set<String> roleNames;
    private Set<String> userNames;
    private boolean enabled;

    // Constructor from entity
    public EndpointPermissionDTO(com.example.eac_backend_project.entity.inventory.EndpointPermission entity) {
        this.id = entity.getId();
        this.httpMethod = entity.getHttpMethod();
        this.endpointPattern = entity.getEndpointPattern();
        this.description = entity.getDescription();
        this.enabled = entity.isEnabled();

        if (entity.getRoles() != null) {
            this.roleNames = entity.getRoles().stream()
                    .map(role -> role.getName())
                    .collect(Collectors.toSet());
        }

        if (entity.getUsers() != null) {
            this.userNames = entity.getUsers().stream()
                    .map(user -> user.getUserName())
                    .collect(Collectors.toSet());
        }
    }
}