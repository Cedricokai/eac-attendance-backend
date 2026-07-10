package com.example.eac_backend_project.entity.attendance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "leave_requests")
public class Leave {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // In Leave.java - ensure this is correct
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Employee employee;

    @ManyToOne
    @JoinColumn(name = "attendance_id")
    private Attendance attendance;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "overtime_id")
    private Overtime overtime;

    private LocalDate startDate;
    private LocalDate endDate;
    private String leaveType;
    private String reason;
    private String status;

    // Approval tracking
    private String supervisorStatus;
    private String supervisorFeedback;
    private LocalDateTime supervisorActionDate;

    private String plannerStatus;
    private String plannerFeedback;
    private LocalDateTime plannerActionDate;

    private String hrStatus;
    private String hrFeedback;
    private LocalDateTime hrActionDate;

    // File attachment fields
    private String attachmentFileName;
    private String attachmentFileType;
    private Long attachmentFileSize;

    // New field for total leave hours
    @Column(name = "total_leave_hours")
    private Double totalLeaveHours;

    @Column(name = "is_deducted")
    private Boolean isDeducted = false;

    @Column(name = "deducted_days")
    private Integer deductedDays = 0;

    @Column(name = "deduction_date")
    private LocalDate deductionDate;

    // NEW FIELD: Secure token for email approval
    @Column(name = "approval_token", unique = true, length = 100)
    private String approvalToken;

    @Column(name = "token_expiry_date")
    private LocalDateTime tokenExpiryDate;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] attachmentFileContent;

    private LocalDateTime submittedDate;

    @Column(name = "weekend_calculation_method")
    private String weekendCalculationMethod; // "EXCLUDED" or "INCLUDED"

    @PrePersist
    protected void onCreate() {
        submittedDate = LocalDateTime.now();
        status = "Pending";
        supervisorStatus = "Pending";
        plannerStatus = "Pending";
        hrStatus = "Pending";
        // Generate unique approval token for email approvals
        this.approvalToken = java.util.UUID.randomUUID().toString();
        this.tokenExpiryDate = LocalDateTime.now().plusDays(7); // Token valid for 7 days
        // Calculate total leave hours when creating leave
        calculateTotalLeaveHours();
    }

    @PreUpdate
    protected void onUpdate() {
        // Recalculate total leave hours when dates change
        calculateTotalLeaveHours();
    }

    /**
     * Calculate total leave hours based on business days and standard hours
     */
    public void calculateTotalLeaveHours() {
        if (startDate == null || endDate == null) {
            this.totalLeaveHours = 0.0;
            return;
        }

        int businessDays = calculateBusinessDays(startDate, endDate);
        double standardHoursPerDay = 8.0; // Standard 8 hours per day

        this.totalLeaveHours = businessDays * standardHoursPerDay;
    }

    /**
     * Calculate business days (weekdays) between two dates
     */
    // Leave.java - Replace the calculateBusinessDays method
    private int calculateBusinessDays(LocalDate start, LocalDate end) {
        int days = 0;
        LocalDate current = start;

        // Get employee category to determine if weekends should be excluded
        boolean excludeWeekends = true; // default
        if (employee != null && employee.getCategory() != null) {
            Boolean setting = employee.getCategory().getExcludeWeekendsFromLeave();
            excludeWeekends = setting == null ? true : setting;
        }

        while (!current.isAfter(end)) {
            DayOfWeek dayOfWeek = current.getDayOfWeek();
            boolean isWeekend = (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY);

            if (excludeWeekends) {
                // Only count weekdays
                if (!isWeekend) {
                    days++;
                }
            } else {
                // Count all days
                days++;
            }
            current = current.plusDays(1);
        }
        return days;
    }

    /**
     * Get total leave days (business days)
     */
    public int getTotalLeaveDays() {
        if (startDate == null || endDate == null) {
            return 0;
        }
        return calculateBusinessDays(startDate, endDate);
    }

    // Check if token is still valid
    public boolean isTokenValid() {
        return approvalToken != null &&
                tokenExpiryDate != null &&
                LocalDateTime.now().isBefore(tokenExpiryDate);
    }

    // Getters and setters
    public Double getTotalLeaveHours() {
        // Ensure totalLeaveHours is always calculated
        if (totalLeaveHours == null) {
            calculateTotalLeaveHours();
        }
        return totalLeaveHours;
    }

    public void setTotalLeaveHours(Double totalLeaveHours) {
        this.totalLeaveHours = totalLeaveHours;
    }

    // Override setter for dates to trigger recalculation
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
        calculateTotalLeaveHours();
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
        calculateTotalLeaveHours();
    }

    // Helper method to check if leave has attachment
    public boolean hasAttachment() {
        return attachmentFileName != null && !attachmentFileName.isEmpty();
    }

    // Getter and Setter for approvalToken
    public String getApprovalToken() {
        return approvalToken;
    }

    public void setApprovalToken(String approvalToken) {
        this.approvalToken = approvalToken;
    }

    public LocalDateTime getTokenExpiryDate() {
        return tokenExpiryDate;
    }

    public void setTokenExpiryDate(LocalDateTime tokenExpiryDate) {
        this.tokenExpiryDate = tokenExpiryDate;
    }

    // Other getters and setters remain the same...
    public String getAttachmentFileName() {
        return attachmentFileName;
    }

    public void setAttachmentFileName(String attachmentFileName) {
        this.attachmentFileName = attachmentFileName;
    }

    public String getAttachmentFileType() {
        return attachmentFileType;
    }

    public void setAttachmentFileType(String attachmentFileType) {
        this.attachmentFileType = attachmentFileType;
    }

    public byte[] getAttachmentFileContent() {
        return attachmentFileContent;
    }

    public void setAttachmentFileContent(byte[] attachmentFileContent) {
        this.attachmentFileContent = attachmentFileContent;
    }

    public Long getAttachmentFileSize() {
        return attachmentFileSize;
    }

    public void setAttachmentFileSize(Long attachmentFileSize) {
        this.attachmentFileSize = attachmentFileSize;
    }

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

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(String leaveType) {
        this.leaveType = leaveType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSupervisorStatus() {
        return supervisorStatus;
    }

    public void setSupervisorStatus(String supervisorStatus) {
        this.supervisorStatus = supervisorStatus;
    }

    public String getSupervisorFeedback() {
        return supervisorFeedback;
    }

    public void setSupervisorFeedback(String supervisorFeedback) {
        this.supervisorFeedback = supervisorFeedback;
    }

    public LocalDateTime getSupervisorActionDate() {
        return supervisorActionDate;
    }

    public void setSupervisorActionDate(LocalDateTime supervisorActionDate) {
        this.supervisorActionDate = supervisorActionDate;
    }

    public String getPlannerStatus() {
        return plannerStatus;
    }

    public void setPlannerStatus(String plannerStatus) {
        this.plannerStatus = plannerStatus;
    }

    public String getPlannerFeedback() {
        return plannerFeedback;
    }

    public void setPlannerFeedback(String plannerFeedback) {
        this.plannerFeedback = plannerFeedback;
    }

    public LocalDateTime getPlannerActionDate() {
        return plannerActionDate;
    }

    public void setPlannerActionDate(LocalDateTime plannerActionDate) {
        this.plannerActionDate = plannerActionDate;
    }

    public String getHrStatus() {
        return hrStatus;
    }

    public void setHrStatus(String hrStatus) {
        this.hrStatus = hrStatus;
    }

    public String getHrFeedback() {
        return hrFeedback;
    }

    public void setHrFeedback(String hrFeedback) {
        this.hrFeedback = hrFeedback;
    }

    public LocalDateTime getHrActionDate() {
        return hrActionDate;
    }

    public void setHrActionDate(LocalDateTime hrActionDate) {
        this.hrActionDate = hrActionDate;
    }

    public LocalDateTime getSubmittedDate() {
        return submittedDate;
    }

    public void setSubmittedDate(LocalDateTime submittedDate) {
        this.submittedDate = submittedDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Attendance getAttendance() {
        return attendance;
    }

    public void setAttendance(Attendance attendance) {
        this.attendance = attendance;
    }

    public Overtime getOvertime() {
        return overtime;
    }

    public void setOvertime(Overtime overtime) {
        this.overtime = overtime;
    }

    public Boolean getIsDeducted() {
        return isDeducted;
    }

    public void setIsDeducted(Boolean isDeducted) {
        this.isDeducted = isDeducted;
    }

    public Integer getDeductedDays() {
        return deductedDays;
    }

    public void setDeductedDays(Integer deductedDays) {
        this.deductedDays = deductedDays;
    }

    public LocalDate getDeductionDate() {
        return deductionDate;
    }

    public void setDeductionDate(LocalDate deductionDate) {
        this.deductionDate = deductionDate;
    }

    public String getWeekendCalculationMethod() {
        return weekendCalculationMethod;
    }

    public void setWeekendCalculationMethod(String weekendCalculationMethod) {
        this.weekendCalculationMethod = weekendCalculationMethod;
    }
}