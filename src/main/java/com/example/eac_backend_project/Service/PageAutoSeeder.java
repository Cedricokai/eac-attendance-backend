package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.entity.inventory.Page;
import com.example.eac_backend_project.entity.inventory.Role;
import com.example.eac_backend_project.repository.PageRepository;
import com.example.eac_backend_project.repository.RoleRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// PageAutoSeeder.java
@Component
public class PageAutoSeeder {

    @Autowired
    private PageRepository pageRepository;

    @Autowired
    private RoleRepository roleRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void autoSeedPages() {
        System.out.println("=== Auto-seeding pages from application routes ===");

        // List of all pages from your App.js with default configurations
        List<PageDefinition> pageDefinitions = Arrays.asList(
                // Auth Pages
                new PageDefinition("Login", "/", "User login page", "LogIn", true, 1, "PUBLIC"),
                new PageDefinition("Signup", "/signup", "User registration page", "UserPlus", true, 2, "PUBLIC"),

                // Employee Dashboard & Profile
                new PageDefinition("Employee Dashboard", "/employeeDashboard", "Main employee dashboard", "LayoutDashboard", false, 10, "ROLE_EMPLOYEE", "ROLE_SUPERVISOR", "ROLE_PLANNER", "ROLE_HR", "ROLE_ADMIN", "ROLE_INVENTORY", "ROLE_TRANSPORT"),
                new PageDefinition("Employee Profile", "/profile/:id", "Employee profile details", "UserCircle", false, 11, "ROLE_EMPLOYEE", "ROLE_SUPERVISOR", "ROLE_HR", "ROLE_ADMIN"),

                // Attendance Module
                new PageDefinition("Attendance", "/attendance", "Attendance management", "CalendarDays", false, 20, "ROLE_EMPLOYEE", "ROLE_SUPERVISOR", "ROLE_HR", "ROLE_ADMIN"),
                new PageDefinition("Attendance Overview", "/overview", "Attendance overview", "Eye", false, 21, "ROLE_SUPERVISOR", "ROLE_HR", "ROLE_ADMIN"),
                new PageDefinition("Biometric Feed", "/biometricAttendanceFeed", "Biometric attendance feed", "Fingerprint", false, 22, "ROLE_HR", "ROLE_ADMIN"),
                new PageDefinition("Daily Report", "/dailyAttendanceReport", "Daily attendance report", "FileText", false, 23, "ROLE_SUPERVISOR", "ROLE_HR", "ROLE_ADMIN"),
                new PageDefinition("Overtime", "/overtime", "Overtime management", "Clock", false, 24, "ROLE_EMPLOYEE", "ROLE_SUPERVISOR", "ROLE_HR", "ROLE_ADMIN"),
                new PageDefinition("Overtime Request Entry", "/employeeOvertimeRequest", "Submit overtime requests on behalf of employees", "Clock", false, 25, "ROLE_ADMIN"),

                // Leave Management
                new PageDefinition("Leave Request Form", "/leaveRequestForm", "Submit leave request", "CalendarPlus", false, 30, "ROLE_EMPLOYEE", "ROLE_SUPERVISOR", "ROLE_PLANNER", "ROLE_HR", "ROLE_ADMIN"),
                new PageDefinition("Leave Status", "/leave-status", "Check leave status", "CalendarCheck", false, 31, "ROLE_EMPLOYEE", "ROLE_SUPERVISOR", "ROLE_HR", "ROLE_ADMIN"),
                new PageDefinition("Supervisor Dashboard", "/supervisorDashboard", "Supervisor approval dashboard", "UserCheck", false, 32, "ROLE_SUPERVISOR", "ROLE_ADMIN"),
                new PageDefinition("Planner Dashboard", "/plannerDashboard", "Planner approval dashboard", "CalendarRange", false, 33, "ROLE_PLANNER", "ROLE_ADMIN"),
                new PageDefinition("HR Dashboard", "/HRDashboard", "HR management dashboard", "Users", false, 34, "ROLE_HR", "ROLE_ADMIN"),

                // Payroll & Timesheets
                new PageDefinition("Payroll", "/payroll", "Payroll management", "DollarSign", false, 40, "ROLE_HR", "ROLE_ADMIN"),
                new PageDefinition("Payslip", "/payslip", "View payslip", "Receipt", false, 41, "ROLE_EMPLOYEE", "ROLE_HR", "ROLE_ADMIN"),
                new PageDefinition("Timesheets", "/timesheets", "Timesheet management", "Clock", false, 42, "ROLE_EMPLOYEE", "ROLE_SUPERVISOR", "ROLE_HR", "ROLE_ADMIN"),
                new PageDefinition("Generate Invoice", "/generateInvoice", "Generate invoices", "FileInvoice", false, 43, "ROLE_HR", "ROLE_ADMIN"),

                // Inventory Management
                new PageDefinition("Inventory Dashboard", "/InventoryDashboard", "Inventory dashboard", "Package", false, 50, "ROLE_INVENTORY", "ROLE_ADMIN"),
                new PageDefinition("Products", "/products", "Product management", "Box", false, 51, "ROLE_INVENTORY", "ROLE_ADMIN"),
                new PageDefinition("Received Items", "/received", "Received items management", "PackageCheck", false, 52, "ROLE_INVENTORY", "ROLE_ADMIN"),
                new PageDefinition("Outgoing Items", "/outgoing", "Outgoing items management", "PackageX", false, 53, "ROLE_INVENTORY", "ROLE_ADMIN"),
                new PageDefinition("Inventory Request", "/InventoryRequest", "Submit inventory request", "ShoppingCart", false, 54, "ROLE_EMPLOYEE", "ROLE_SUPERVISOR", "ROLE_PLANNER", "ROLE_HR", "ROLE_ADMIN", "ROLE_INVENTORY"),
                new PageDefinition("Product Request Form", "/product-request-form", "Submit product request", "ClipboardList", false, 55, "ROLE_EMPLOYEE", "ROLE_SUPERVISOR", "ROLE_PLANNER", "ROLE_ADMIN"),
                new PageDefinition("PPE Management", "/ppe", "Personal Protective Equipment", "Shield", false, 56, "ROLE_INVENTORY", "ROLE_ADMIN"),

                // Procurement
                new PageDefinition("Procurement Review", "/procurementManagerReview", "Procurement manager review", "ClipboardCheck", false, 60, "ROLE_PROCUREMENT_OFFICER", "ROLE_ADMIN"),
                new PageDefinition("Planner Products Review", "/plannerProductsReview", "Planner products review", "ClipboardCheck", false, 61, "ROLE_PLANNER", "ROLE_ADMIN"),

                // Store Management
                new PageDefinition("Store Officer Approval", "/StoreOfficerApproval", "Store officer approval", "PackageCheck", false, 70, "ROLE_INVENTORY", "ROLE_ADMIN"),
                new PageDefinition("Storekeeper Requests", "/storeKeeperRequests", "Storekeeper requests management", "Clipboard", false, 71, "ROLE_INVENTORY", "ROLE_ADMIN"),

                // Cost Center & Reports
                new PageDefinition("Cost Center Management", "/CostCenterManagement", "Cost center management", "PieChart", false, 80, "ROLE_ADMIN", "ROLE_PLANNER"),
                new PageDefinition("Jobs Management", "/JobsManagement", "Jobs management", "Briefcase", false, 81, "ROLE_PLANNER", "ROLE_ADMIN"),
                new PageDefinition("Inventory Reports", "/inventory-reports", "Inventory reports dashboard", "BarChart", false, 82, "ROLE_INVENTORY", "ROLE_ADMIN"),
                new PageDefinition("General Reports", "/reports", "System reports", "FileBarChart", false, 83, "ROLE_SUPERVISOR", "ROLE_HR", "ROLE_ADMIN"),

                // Transport Module
                new PageDefinition("Transport Dashboard", "/admindashboard", "Transport management dashboard", "Truck", false, 90, "ROLE_TRANSPORT", "ROLE_ADMIN"),
                new PageDefinition("Drivers", "/drivers", "Drivers management", "UserCog", false, 91, "ROLE_TRANSPORT", "ROLE_ADMIN"),
                new PageDefinition("Vehicles", "/vehicles", "Vehicles management", "Car", false, 92, "ROLE_TRANSPORT", "ROLE_ADMIN"),
                new PageDefinition("Fuel Management", "/fuel", "Fuel consumption tracking", "Fuel", false, 93, "ROLE_TRANSPORT", "ROLE_ADMIN"),
                new PageDefinition("Maintenance", "/maintenance", "Vehicle maintenance", "Wrench", false, 94, "ROLE_TRANSPORT", "ROLE_ADMIN"),
                new PageDefinition("Transport Users", "/usersmanagement", "Transport users management", "Users", false, 95, "ROLE_TRANSPORT", "ROLE_ADMIN"),
                new PageDefinition("Transport Reports", "/transportReports", "Transport reports", "FileBarChart", false, 96, "ROLE_TRANSPORT", "ROLE_ADMIN"),

                // Admin & Settings
                new PageDefinition("Central Dashboard", "/centralizedDashboard", "Central dashboard", "LayoutDashboard", false, 100, "ROLE_ADMIN"),
                new PageDefinition("User Management", "/Userpage", "User management panel", "Users", false, 101, "ROLE_ADMIN"),
                new PageDefinition("Page Permissions", "/pagePermissionManagement", "Page permission management", "ShieldCheck", false, 102, "ROLE_ADMIN"),
                new PageDefinition("Settings", "/settingspage", "System settings", "Settings", false, 103, "ROLE_ADMIN"),
                new PageDefinition("History", "/history", "System history log", "History", false, 104, "ROLE_ADMIN"),
                new PageDefinition("Search", "/search", "Global search", "Search", false, 105, "ROLE_ADMIN", "ROLE_HR", "ROLE_SUPERVISOR")
        );

        for (PageDefinition definition : pageDefinitions) {
            // Check if page already exists
            if (!pageRepository.findByPath(definition.getPath()).isPresent()) {
                Page page = new Page();
                page.setName(definition.getName());
                page.setPath(definition.getPath());
                page.setDescription(definition.getDescription());
                page.setIconName(definition.getIconName());
                page.setIsPublic(definition.isPublic());
                page.setDisplayOrder(definition.getOrder());

                // Assign roles
                Set<Role> roles = new HashSet<>();
                for (String roleName : definition.getRoles()) {
                    if (roleName.equals("PUBLIC")) {
                        continue; // Public pages don't need role assignment
                    }

                    String formattedRoleName = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
                    roleRepository.findByName(formattedRoleName)
                            .ifPresent(roles::add);
                }
                page.setRoles(roles);

                pageRepository.save(page);
                System.out.println("Created page: " + definition.getName() + " (" + definition.getPath() + ")");
            }
        }

        System.out.println("=== Page auto-seeding completed ===");
    }

    // Helper class for page definitions
    private static class PageDefinition {
        private final String name;
        private final String path;
        private final String description;
        private final String iconName;
        private final boolean isPublic;
        private final int order;
        private final String[] roles;

        public PageDefinition(String name, String path, String description, String iconName,
                              boolean isPublic, int order, String... roles) {
            this.name = name;
            this.path = path;
            this.description = description;
            this.iconName = iconName;
            this.isPublic = isPublic;
            this.order = order;
            this.roles = roles;
        }

        // Getters
        public String getName() { return name; }
        public String getPath() { return path; }
        public String getDescription() { return description; }
        public String getIconName() { return iconName; }
        public boolean isPublic() { return isPublic; }
        public int getOrder() { return order; }
        public String[] getRoles() { return roles; }
    }
}
