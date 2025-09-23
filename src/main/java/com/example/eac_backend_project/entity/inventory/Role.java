package com.example.eac_backend_project.entity.inventory;

public enum Role {
    ROLE_ADMIN,
    ROLE_INVENTORY,
    ROLE_HR,
    ROLE_CUSTOMER,
    ROLE_SUPERVISOR,
    ROLE_PLANNER;

    public static Role getRoleByName(String name) {
        return Role.valueOf(name.toUpperCase());
    }
}