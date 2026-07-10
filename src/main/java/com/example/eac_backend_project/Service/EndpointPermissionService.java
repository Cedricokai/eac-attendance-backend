package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.entity.inventory.EndpointPermission;
import com.example.eac_backend_project.entity.inventory.Role;
import com.example.eac_backend_project.entity.inventory.User;
import com.example.eac_backend_project.repository.EndpointPermissionRepository;
import com.example.eac_backend_project.repository.RoleRepository;
import com.example.eac_backend_project.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EndpointPermissionService {

    @Autowired
    private EndpointPermissionRepository endpointPermissionRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    public List<EndpointPermission> getAllEndpointPermissions() {
        return endpointPermissionRepository.findAll();
    }

    public List<EndpointPermission> getAllActivePermissions() {
        return endpointPermissionRepository.findAllActiveWithRolesAndUsers();
    }

    public List<EndpointPermission> getEndpointsByRole(String roleName) {
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));

        return getAllActivePermissions().stream()
                .filter(p -> p.getRoles().contains(role))
                .toList();
    }

    public List<EndpointPermission> getEndpointsByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        return getAllActivePermissions().stream()
                .filter(p -> p.getUsers().contains(user))
                .toList();
    }

    public EndpointPermission createEndpointPermission(EndpointPermission permission) {
        permission.setEnabled(true);
        return endpointPermissionRepository.save(permission);
    }

    public EndpointPermission updateEndpointPermission(Long id, EndpointPermission permission) {
        permission.setId(id);
        return endpointPermissionRepository.save(permission);
    }

    public void deleteEndpointPermission(Long id) {
        EndpointPermission permission = endpointPermissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Endpoint permission not found"));
        permission.setEnabled(false);
        endpointPermissionRepository.save(permission);
    }

    public EndpointPermission assignToRole(Long endpointId, Long roleId) {
        EndpointPermission permission = endpointPermissionRepository.findById(endpointId)
                .orElseThrow(() -> new RuntimeException("Endpoint permission not found"));
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));
        permission.getRoles().add(role);
        return endpointPermissionRepository.save(permission);
    }

    public EndpointPermission removeFromRole(Long endpointId, Long roleId) {
        EndpointPermission permission = endpointPermissionRepository.findById(endpointId)
                .orElseThrow(() -> new RuntimeException("Endpoint permission not found"));
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));
        permission.getRoles().remove(role);
        return endpointPermissionRepository.save(permission);
    }

    public EndpointPermission assignToUser(Long endpointId, Long userId) {
        EndpointPermission permission = endpointPermissionRepository.findById(endpointId)
                .orElseThrow(() -> new RuntimeException("Endpoint permission not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        permission.getUsers().add(user);
        return endpointPermissionRepository.save(permission);
    }

    public EndpointPermission removeFromUser(Long endpointId, Long userId) {
        EndpointPermission permission = endpointPermissionRepository.findById(endpointId)
                .orElseThrow(() -> new RuntimeException("Endpoint permission not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        permission.getUsers().remove(user);
        return endpointPermissionRepository.save(permission);
    }
}

