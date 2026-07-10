package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.entity.inventory.Role;
import com.example.eac_backend_project.entity.inventory.SystemRole;
import com.example.eac_backend_project.repository.RoleRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class RoleSeeder {

    private final RoleRepository roleRepository;

    public RoleSeeder(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seedRoles() {
        // Define all system roles that should exist
        String[] systemRoles = {
                "ROLE_ADMIN",
                "ROLE_CUSTOMER",
                "ROLE_HR",
                "ROLE_INVENTORY",
                "ROLE_SUPERVISOR",
                "ROLE_PLANNER",
                "ROLE_PROCUREMENT_OFFICER",
                "ROLE_TRANSPORT",
                "ROLE_MANAGER",
                "ROLE_USER"
        };

        for (String roleName : systemRoles) {
            roleRepository.findByName(roleName)
                    .orElseGet(() -> {
                        Role role = new Role(roleName, true);
                        return roleRepository.save(role);
                    });
        }
    }
}