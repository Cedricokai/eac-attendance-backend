package com.example.eac_backend_project.entity.attendance;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "timesheets")
public class Timesheet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne
    @JoinColumn(name = "overview_id") // Remove nullable = false to make it optional
    private Overview overview;

    @ManyToOne
    @JoinColumn(name = "job_id") // Make job optional too if needed
    private Job job;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "attendance_code")  // Add this column
    private String attendanceCode;

    @Column(name = "regular_hours")
    private double regularHours;

    @Column(name = "overtime_hours")
    private double overtimeHours;

    @Column(name = "break_hours")
    private double breakHours;

    @Column(name = "total_hours")
    private double totalHours;

    @Column(name = "earnings")
    private Double earnings;

    @Enumerated(EnumType.STRING)
    private TimesheetStatus status = TimesheetStatus.PENDING;

    @Column(name = "notes")
    private String notes;

    @Column(name = "source_document_missing")
    private Boolean sourceDocumentMissing = false;

    @Column(name = "source_document_missing_reason", length = 500)
    private String sourceDocumentMissingReason;

    @Column(name = "source_document_missing_flagged_at")
    private LocalDateTime sourceDocumentMissingFlaggedAt;

    @Column(name = "source_document_missing_flagged_by")
    private String sourceDocumentMissingFlaggedBy;

    @Column(name = "source_document_type")
    private String sourceDocumentType;  // e.g., "HARD_COPY", "EXCEL", "MANUAL", null

    @Column(name = "source_document_reference", length = 200)
    private String sourceDocumentReference;  // e.g., file name, batch ID, timesheet ID

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verified_by")
    private String verifiedBy;

    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    private String payrollApprovalStatus = "DRAFT";
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    private String payrollApprovedBy;
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    private LocalDateTime payrollApprovedAt;
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    private String payrollSubmittedBy;
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    private LocalDateTime payrollSubmittedAt;
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    private String payrollDecisionReason;
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String payrollReviewFingerprint;
    @Version
    @Column(columnDefinition = "bigint default 0")
    private Long approvalVersion;

    @com.fasterxml.jackson.annotation.JsonIgnore
    public String payrollFingerprint() {
        String work = String.join("|", String.valueOf(employee == null ? null : employee.getId()),
                String.valueOf(job == null ? null : job.getId()), String.valueOf(date),
                String.valueOf(attendanceCode), String.valueOf(regularHours), String.valueOf(overtimeHours),
                String.valueOf(breakHours));
        if (job != null && Boolean.TRUE.equals(job.getDailyHoursTopUpEnabled())) {
            work += "|daily-top-up|" + job.getDailyHoursTopUpThreshold() + "|" + job.getDailyHoursTopUpTarget() + "|" + job.getStandardWorkHours();
        }
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(work.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isPayrollApproved() {
        return "APPROVED".equals(payrollApprovalStatus) && payrollFingerprint().equals(payrollReviewFingerprint)
                && payrollApprovedBy != null && payrollApprovedAt != null;
    }
    @PrePersist
    public void initializePayrollApproval() { resetPayrollApproval(); }
    @PreUpdate
    public void invalidateChangedPayrollApproval() {
        if (payrollApprovalStatus == null || (!"DRAFT".equals(payrollApprovalStatus)
                && !payrollFingerprint().equals(payrollReviewFingerprint))) resetPayrollApproval();
    }
    public void resetPayrollApproval() {
        payrollApprovalStatus = "DRAFT";
        payrollReviewFingerprint = null;
        payrollApprovedBy = null; payrollApprovedAt = null;
        payrollSubmittedBy = null; payrollSubmittedAt = null; payrollDecisionReason = null;
    }

    // Constructors
    public Timesheet() {}

    public Timesheet(Employee employee, LocalDate date, double regularHours, double overtimeHours) {
        this.employee = employee;
        this.date = date;
        this.regularHours = regularHours;
        this.overtimeHours = overtimeHours;
        this.totalHours = regularHours + overtimeHours;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public Overview getOverview() {
        return overview;
    }

    public void setOverview(Overview overview) {
        this.overview = overview;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public double getRegularHours() {
        return regularHours;
    }

    public void setRegularHours(double regularHours) {
        this.regularHours = regularHours;
        calculateTotalHours();
    }

    public double getOvertimeHours() {
        return overtimeHours;
    }

    public void setOvertimeHours(double overtimeHours) {
        this.overtimeHours = overtimeHours;
        calculateTotalHours();
    }

    public double getBreakHours() {
        return breakHours;
    }

    public void setBreakHours(double breakHours) {
        this.breakHours = breakHours;
    }

    public double getTotalHours() {
        return totalHours;
    }

    public void setTotalHours(double totalHours) {
        this.totalHours = totalHours;
    }

    public Double getEarnings() {
        return earnings;
    }

    public void setEarnings(Double earnings) {
        this.earnings = earnings;
    }

    public TimesheetStatus getStatus() {
        return status;
    }

    public void setStatus(TimesheetStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    // Helper method to calculate total hours
    private void calculateTotalHours() {
        this.totalHours = this.regularHours + this.overtimeHours;
    }

    // Convenience method to update hours and recalculate total
    public void updateHours(double regularHours, double overtimeHours, double breakHours) {
        this.regularHours = regularHours;
        this.overtimeHours = overtimeHours;
        this.breakHours = breakHours;
        calculateTotalHours();
    }

    @Override
    public String toString() {
        return "Timesheet{" +
                "id=" + id +
                ", employee=" + (employee != null ? employee.getId() : "null") +
                ", date=" + date +
                ", regularHours=" + regularHours +
                ", overtimeHours=" + overtimeHours +
                ", totalHours=" + totalHours +
                ", earnings=" + earnings +
                ", status=" + status +
                '}';
    }


}