package com.example.eac_backend_project.entity.inventory;

public enum SystemRole {
    ROLE_ADMIN,
    ROLE_INVENTORY,
    ROLE_HR,
    ROLE_CUSTOMER,
    ROLE_SUPERVISOR,
    ROLE_PLANNER,
    ROLE_PROCUREMENT_OFFICER,
    ROLE_TRANSPORT;

    public static SystemRole getRoleByName(String name) {
        return SystemRole.valueOf(name.toUpperCase());
    }
}