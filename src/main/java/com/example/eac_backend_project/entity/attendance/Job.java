package com.example.eac_backend_project.entity.attendance;

import com.example.eac_backend_project.entity.inventory.CostCenter;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "jobs")
@Data
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "cost_center_id")
    private CostCenter costCenter;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String client;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false)
    private BillingCycle billingCycle;

    @Column(name = "billing_reminder_enabled", nullable = false)
    private boolean billingReminderEnabled = false;

    // Add this method to Job.java - checks if anchor day billing is enabled
    public boolean isAnchorDayBilling() {
        return billingAnchorDay != null && billingAnchorDay >= 1 && billingAnchorDay <= 31;
    }

    @Lob
    @Column(name = "billing_reminder_emails", columnDefinition = "TEXT")
    private String billingReminderEmails;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(name = "next_billing_date")
    private LocalDate nextBillingDate;

    private Double budget;

    @Column(name = "hourly_rate")
    private Double hourlyRate;

    @Column(name = "standard_work_hours")
    private Integer standardWorkHours = 8;

    @Column(name = "work_start_time")
    private java.time.LocalTime workStartTime;

    @Column(name = "billing_hours")
    private Double billingHours;

    @Column(name = "expected_overtime_hours")
    private Double expectedOvertimeHours = 0.0;

    @Column(name = "daily_hours_top_up_enabled", columnDefinition = "boolean default false")
    private Boolean dailyHoursTopUpEnabled = false;
    @Column(name = "daily_hours_top_up_threshold")
    private Double dailyHoursTopUpThreshold = 9.3;
    @Column(name = "daily_hours_top_up_target")
    private Double dailyHoursTopUpTarget = 10.0;



    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status = JobStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_id")
    @ToString.Exclude
    private Employee supervisor;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<JobProduct> products = new ArrayList<>();

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<JobAttachment> attachments = new ArrayList<>();

    @OneToMany(mappedBy = "job")
    @ToString.Exclude
    private List<Employee> employees = new ArrayList<>();

    @Column(name = "bill_on_15_next_month", nullable = false)
    private Boolean billOn15NextMonth = false;

    @Column(name = "billing_anchor_day")
    private Integer billingAnchorDay;

    @Column(name = "overtime_multiplier")
    private Double overtimeMultiplier;

    @Column(name = "apply_overtime_multiplier_to_hours", nullable = false, columnDefinition = "boolean default false")
    private Boolean applyOvertimeMultiplierToHours = false;

    @Column(name = "enforce_minimum_overtime", nullable = false, columnDefinition = "boolean default false")
    private Boolean enforceMinimumOvertime = false;

    @Column(name = "use_configured_attendance_hours", nullable = false, columnDefinition = "boolean default false")
    private Boolean useConfiguredAttendanceHours = false;

    @Column(name = "minimum_overtime_hours")
    private Double minimumOvertimeHours;

    @Column(name = "auto_approve_overtime", nullable = false, columnDefinition = "boolean default false")
    private Boolean autoApproveOvertime = false;

    @Lob
    @Column(name = "job_notification_emails", columnDefinition = "TEXT")
    private String jobNotificationEmails;

    // ========== NEW REMINDER CONFIGURATION FIELDS ==========

    @Column(name = "enable_advance_reminders")
    private Boolean enableAdvanceReminders = true;

    @Column(name = "reminder_days_before", length = 100)
    private String reminderDaysBefore = "7,3,1";

    @Column(name = "reminder_time_hour")
    private Integer reminderTimeHour = 8;

    @Column(name = "reminder_time_minute")
    private Integer reminderTimeMinute = 0;

    @Column(name = "enable_overdue_reminders")
    private Boolean enableOverdueReminders = true;

    @Column(name = "overdue_reminder_days")
    private Integer overdueReminderDays = 1;

    public Job() {}

    public Job(String name, String description, String client, LocalDate startDate) {
        this.name = name;
        this.description = description;
        this.client = client;
        this.startDate = startDate;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (reminderTimeHour == null) reminderTimeHour = 8;
        if (reminderTimeMinute == null) reminderTimeMinute = 0;
        if (enableAdvanceReminders == null) enableAdvanceReminders = true;
        if (reminderDaysBefore == null) reminderDaysBefore = "7,3,1";
        if (enableOverdueReminders == null) enableOverdueReminders = true;
        if (overdueReminderDays == null) overdueReminderDays = 1;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum JobStatus {
        ACTIVE, COMPLETED, ON_HOLD, CANCELLED
    }

    public enum BillingCycle {
        WEEKLY, BI_WEEKLY, MONTHLY, QUARTERLY
    }

    // ========== GETTERS AND SETTERS ==========

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CostCenter getCostCenter() {
        return costCenter;
    }

    public void setCostCenter(CostCenter costCenter) {
        this.costCenter = costCenter;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public BillingCycle getBillingCycle() {
        return billingCycle;
    }

    public void setBillingCycle(BillingCycle billingCycle) {
        this.billingCycle = billingCycle;
    }

    public boolean isBillingReminderEnabled() {
        return billingReminderEnabled;
    }

    public void setBillingReminderEnabled(boolean billingReminderEnabled) {
        this.billingReminderEnabled = billingReminderEnabled;
    }

    public String getBillingReminderEmails() {
        return billingReminderEmails;
    }

    public void setBillingReminderEmails(String billingReminderEmails) {
        this.billingReminderEmails = billingReminderEmails;
    }

    public LocalDate getNextBillingDate() {
        return nextBillingDate;
    }

    public void setNextBillingDate(LocalDate nextBillingDate) {
        this.nextBillingDate = nextBillingDate;
    }

    public Double getBudget() {
        return budget;
    }

    public void setBudget(Double budget) {
        this.budget = budget;
    }

    public Double getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(Double hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public Integer getStandardWorkHours() {
        return standardWorkHours;
    }

    public void setStandardWorkHours(Integer standardWorkHours) {
        this.standardWorkHours = standardWorkHours;
    }

    public Double getBillingHours() { return billingHours; }
    public void setBillingHours(Double billingHours) { this.billingHours = billingHours; }
    public Double getExpectedOvertimeHours() { return expectedOvertimeHours; }
    public void setExpectedOvertimeHours(Double expectedOvertimeHours) { this.expectedOvertimeHours = expectedOvertimeHours; }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Employee getSupervisor() {
        return supervisor;
    }

    public void setSupervisor(Employee supervisor) {
        this.supervisor = supervisor;
    }

    public List<JobProduct> getProducts() {
        return products;
    }

    public void setProducts(List<JobProduct> products) {
        this.products = products;
    }

    public List<JobAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<JobAttachment> attachments) {
        this.attachments = attachments;
    }

    public List<Employee> getEmployees() {
        return employees;
    }

    public void setEmployees(List<Employee> employees) {
        this.employees = employees;
    }

    public Boolean getBillOn15NextMonth() {
        return billOn15NextMonth;
    }

    public boolean isBillOn15NextMonth() {
        return Boolean.TRUE.equals(billOn15NextMonth);
    }

    public void setBillOn15NextMonth(Boolean billOn15NextMonth) {
        this.billOn15NextMonth = billOn15NextMonth;
        if (Boolean.TRUE.equals(billOn15NextMonth)) {
            this.billingAnchorDay = 15;
        }
    }

    public Integer getBillingAnchorDay() {
        return billingAnchorDay;
    }

    public void setBillingAnchorDay(Integer billingAnchorDay) {
        this.billingAnchorDay = billingAnchorDay;
    }

    public Double getOvertimeMultiplier() {
        return overtimeMultiplier;
    }

    public void setOvertimeMultiplier(Double overtimeMultiplier) {
        this.overtimeMultiplier = overtimeMultiplier;
    }

    public Boolean getApplyOvertimeMultiplierToHours() {
        return applyOvertimeMultiplierToHours;
    }

    public void setApplyOvertimeMultiplierToHours(Boolean applyOvertimeMultiplierToHours) {
        this.applyOvertimeMultiplierToHours = applyOvertimeMultiplierToHours;
    }

    public Boolean getEnforceMinimumOvertime() {
        return enforceMinimumOvertime;
    }

    public void setEnforceMinimumOvertime(Boolean enforceMinimumOvertime) {
        this.enforceMinimumOvertime = enforceMinimumOvertime;
    }

    public Double getMinimumOvertimeHours() {
        return minimumOvertimeHours;
    }

    public void setMinimumOvertimeHours(Double minimumOvertimeHours) {
        this.minimumOvertimeHours = minimumOvertimeHours;
    }

    public Boolean getAutoApproveOvertime() {
        return autoApproveOvertime;
    }

    public void setAutoApproveOvertime(Boolean autoApproveOvertime) {
        this.autoApproveOvertime = autoApproveOvertime;
    }

    public String getJobNotificationEmails() {
        return jobNotificationEmails;
    }

    public void setJobNotificationEmails(String jobNotificationEmails) {
        this.jobNotificationEmails = jobNotificationEmails;
    }

    // ========== NEW GETTERS AND SETTERS ==========

    public Boolean getEnableAdvanceReminders() {
        return enableAdvanceReminders;
    }

    public void setEnableAdvanceReminders(Boolean enableAdvanceReminders) {
        this.enableAdvanceReminders = enableAdvanceReminders;
    }

    public String getReminderDaysBefore() {
        return reminderDaysBefore;
    }

    public void setReminderDaysBefore(String reminderDaysBefore) {
        this.reminderDaysBefore = reminderDaysBefore;
    }

    public Integer getReminderTimeHour() {
        return reminderTimeHour != null ? reminderTimeHour : 8;
    }

    public void setReminderTimeHour(Integer reminderTimeHour) {
        this.reminderTimeHour = reminderTimeHour;
    }

    public Integer getReminderTimeMinute() {
        return reminderTimeMinute != null ? reminderTimeMinute : 0;
    }

    public void setReminderTimeMinute(Integer reminderTimeMinute) {
        this.reminderTimeMinute = reminderTimeMinute;
    }

    public Boolean getEnableOverdueReminders() {
        return enableOverdueReminders;
    }

    public void setEnableOverdueReminders(Boolean enableOverdueReminders) {
        this.enableOverdueReminders = enableOverdueReminders;
    }

    public Integer getOverdueReminderDays() {
        return overdueReminderDays;
    }

    public void setOverdueReminderDays(Integer overdueReminderDays) {
        this.overdueReminderDays = overdueReminderDays;
    }
}
