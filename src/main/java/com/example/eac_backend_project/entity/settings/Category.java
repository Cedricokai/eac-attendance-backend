package com.example.eac_backend_project.entity.settings;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "categories")
@Data
public class Category {
    // Sunday=0 through Saturday=6; null/INHERIT uses the category for employees.
    @Column(name = "weekly_off_days")
    private String weeklyOffDays;

    public void setWeeklyOffDays(String value) {
        if (value != null && !value.equals("INHERIT") && !value.matches("[0-6](,[0-6])*|")) {
            throw new IllegalArgumentException("Invalid weekly off days");
        }
        this.weeklyOffDays = value;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "standard_rate_hours")
    private Integer standardRateHours = 8;

    @Column(name = "use_non_taxable_allowances")
    private Boolean useNonTaxableAllowances = false;

    private String workStartTime = "09:00";
    private java.time.LocalTime absenceCutoffTime;
    private Boolean overnightShift = false;

    @Column(name = "exclude_weekends_from_leave")
    private Boolean excludeWeekendsFromLeave = true;

    @Column(name = "supervisor_emails", columnDefinition = "TEXT")
    private String supervisorEmails;  // Comma-separated emails for supervisor approval

    @Column(name = "planner_emails", columnDefinition = "TEXT")
    private String plannerEmails;  // Comma-separated emails for planner approval

    @Column(name = "hr_emails", columnDefinition = "TEXT")
    private String hrEmails;  // Comma-separated emails for HR approval

    @Column(name = "use_custom_approvers", columnDefinition = "boolean default false")
    private Boolean useCustomApprovers = false;

    @Column(name = "annual_leave_days")
    private Integer annualLeaveDays = 20;

    @Column(name = "skip_auto_absent")
    private Boolean skipAutoAbsent = false;

    @Column(name = "apply_withholding_tax", columnDefinition = "boolean default false")
    private Boolean applyWithholdingTax = false;

    @Column(name = "weekend_attendance_as_overtime", columnDefinition = "boolean default false")
    private Boolean weekendAttendanceAsOvertime = false;

    // Constructors
    public Category() {}

    public Category(String name, Integer standardRateHours, Boolean useNonTaxableAllowances) {
        this.name = name;
        this.standardRateHours = standardRateHours;
        this.useNonTaxableAllowances = useNonTaxableAllowances;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getStandardRateHours() { return standardRateHours; }
    public void setStandardRateHours(Integer standardRateHours) { this.standardRateHours = standardRateHours; }

    public Boolean getUseNonTaxableAllowances() { return useNonTaxableAllowances; }
    public void setUseNonTaxableAllowances(Boolean useNonTaxableAllowances) { this.useNonTaxableAllowances = useNonTaxableAllowances; }

    // 🔽 Add this toLowerCase helper
    public String toLowerCase() {
        return name == null ? null : name.toLowerCase();
    }

    // 🔽 Add this toString()
    @Override
    public String toString() {
        return name;
    }
}
