package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.Service.EndpointPermissionService;
import com.example.eac_backend_project.dto.EndpointPermissionDTO;
import com.example.eac_backend_project.entity.inventory.EndpointPermission;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/endpoint-permissions")
public class EndpointPermissionController {

    @Autowired
    private EndpointPermissionService endpointPermissionService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<EndpointPermissionDTO>> getAllEndpointPermissions() {
        List<EndpointPermissionDTO> dtos = endpointPermissionService.getAllEndpointPermissions()
                .stream()
                .map(EndpointPermissionDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/roles/{roleName}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<EndpointPermissionDTO>> getEndpointsByRole(@PathVariable String roleName) {
        List<EndpointPermissionDTO> dtos = endpointPermissionService.getEndpointsByRole(roleName)
                .stream()
                .map(EndpointPermissionDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<EndpointPermissionDTO>> getEndpointsByUser(@PathVariable Long userId) {
        List<EndpointPermissionDTO> dtos = endpointPermissionService.getEndpointsByUser(userId)
                .stream()
                .map(EndpointPermissionDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<EndpointPermissionDTO> createEndpointPermission(@RequestBody EndpointPermissionRequest request) {
        EndpointPermission permission = new EndpointPermission();
        permission.setHttpMethod(request.getHttpMethod());
        permission.setEndpointPattern(request.getEndpointPattern());
        permission.setDescription(request.getDescription());
        permission.setEnabled(true);

        EndpointPermission saved = endpointPermissionService.createEndpointPermission(permission);
        return ResponseEntity.ok(new EndpointPermissionDTO(saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<EndpointPermissionDTO> updateEndpointPermission(
            @PathVariable Long id,
            @RequestBody EndpointPermissionRequest request) {
        EndpointPermission permission = new EndpointPermission();
        permission.setId(id);
        permission.setHttpMethod(request.getHttpMethod());
        permission.setEndpointPattern(request.getEndpointPattern());
        permission.setDescription(request.getDescription());

        EndpointPermission updated = endpointPermissionService.updateEndpointPermission(id, permission);
        return ResponseEntity.ok(new EndpointPermissionDTO(updated));
    }

    @PutMapping("/{endpointId}/assign-to-role/{roleId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<EndpointPermissionDTO> assignToRole(@PathVariable Long endpointId, @PathVariable Long roleId) {
        EndpointPermission permission = endpointPermissionService.assignToRole(endpointId, roleId);
        return ResponseEntity.ok(new EndpointPermissionDTO(permission));
    }

    @PutMapping("/{endpointId}/remove-from-role/{roleId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<EndpointPermissionDTO> removeFromRole(@PathVariable Long endpointId, @PathVariable Long roleId) {
        EndpointPermission permission = endpointPermissionService.removeFromRole(endpointId, roleId);
        return ResponseEntity.ok(new EndpointPermissionDTO(permission));
    }

    @PutMapping("/{endpointId}/assign-to-user/{userId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<EndpointPermissionDTO> assignToUser(@PathVariable Long endpointId, @PathVariable Long userId) {
        EndpointPermission permission = endpointPermissionService.assignToUser(endpointId, userId);
        return ResponseEntity.ok(new EndpointPermissionDTO(permission));
    }

    @PutMapping("/{endpointId}/remove-from-user/{userId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<EndpointPermissionDTO> removeFromUser(@PathVariable Long endpointId, @PathVariable Long userId) {
        EndpointPermission permission = endpointPermissionService.removeFromUser(endpointId, userId);
        return ResponseEntity.ok(new EndpointPermissionDTO(permission));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> deleteEndpointPermission(@PathVariable Long id) {
        endpointPermissionService.deleteEndpointPermission(id);
        return ResponseEntity.ok().build();
    }


    // Simple request class to avoid Jackson issues
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class EndpointPermissionRequest {
        private String httpMethod;
        private String endpointPattern;
        private String description;
    }
}