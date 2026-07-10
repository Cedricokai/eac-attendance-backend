package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.entity.inventory.Role;
import com.example.eac_backend_project.repository.RoleRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public Role getOrCreateRole(String roleName) {
        // Ensure role name has ROLE_ prefix
        String formattedRoleName = roleName.trim().toUpperCase();
        if (!formattedRoleName.startsWith("ROLE_")) {
            formattedRoleName = "ROLE_" + formattedRoleName;
        }

        String finalFormattedRoleName = formattedRoleName;
        return roleRepository.findByName(formattedRoleName)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(finalFormattedRoleName);
                    role.setSystem(isSystemRole(finalFormattedRoleName));
                    return roleRepository.save(role);
                });
    }

    private boolean isSystemRole(String roleName) {
        List<String> systemRoles = Arrays.asList(
                "ROLE_ADMIN", "ROLE_CUSTOMER", "ROLE_HR", "ROLE_INVENTORY",
                "ROLE_SUPERVISOR", "ROLE_PLANNER", "ROLE_PROCUREMENT_OFFICER",
                "ROLE_TRANSPORT", "ROLE_MANAGER"
        );
        return systemRoles.contains(roleName);
    }

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }
}