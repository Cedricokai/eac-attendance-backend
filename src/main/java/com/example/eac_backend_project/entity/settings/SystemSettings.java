package com.example.eac_backend_project.entity.settings;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;

@Entity
@Data
@Table(name = "system_settings")
public class SystemSettings {
    private java.math.BigDecimal pettyCashPurchaseLimit;
    public java.math.BigDecimal getPettyCashPurchaseLimit() { return pettyCashPurchaseLimit; }
    public void setPettyCashPurchaseLimit(java.math.BigDecimal value) { pettyCashPurchaseLimit = value; }


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double defaultOvertimeMultiplier;
    private Double sundayOvertimeMultiplier = 2.0;
    private Double holidayOvertimeMultiplier = 2.5;
    private Boolean enableTimeAndHalfAfter8Hours = true;
    private Double timeAndHalfMultiplier = 1.5;

    @Column(name = "withholding_tax_rate")
    private Double withholdingTaxRate = 0.0;

    @Column(name = "withholding_tax_enabled")
    private Boolean withholdingTaxEnabled = false;

    private Double hourlyRate = 10.0;
    private Double overtimeHourlyRate = 15.0;
    private Integer standardWorkHours = 8;

    private String weekendDays = "0,6";

    private Boolean doubleTimeOnSunday = true;
    private Boolean timeAndHalfAfter8Hours = true;
    private Double weekendRate = 1.0;
    private Double holidayRate = 1.0;

    @Column(name = "billing_reminder_emails", columnDefinition = "TEXT")
    private String billingReminderEmails;

    @Column(name = "billing_reminder_enabled")
    private Boolean billingReminderEnabled = true;

    @Column(name = "late_arrival_time", length = 10)
    private String lateArrivalTime = "09:00";

    private String companyName;
    private String companyAddress;
    private String companyPhone;
    private String companyEmail;

    @Column(columnDefinition = "TEXT")
    private String loanRequestRecipients;

    @Column(columnDefinition = "TEXT")
    private String loanApprovalRecipients;

    @Column(columnDefinition = "TEXT")
    private String loanRejectionRecipients;

    @Column(columnDefinition = "TEXT")
    private String leaveRequestRecipients;

    @Column(columnDefinition = "TEXT")
    private String attendanceAlertRecipients;

    @Column(columnDefinition = "TEXT")
    private String lateArrivalRecipients;

    @Column(columnDefinition = "TEXT")
    private String overtimeRequestRecipients;

    @Column(columnDefinition = "TEXT")
    private String attendanceValidationRecipients;

    @Column(name = "loan_consolidation_recipients", columnDefinition = "TEXT")
    private String loanConsolidationRecipients;

    @Column(columnDefinition = "TEXT")
    private String leaveApprovalRecipients;

    @Column(name = "account_request_recipients", columnDefinition = "TEXT")
    private String accountRequestRecipients;

    @Column(name = "support_request_recipients", columnDefinition = "TEXT")
    private String supportRequestRecipients;

    @Column(name = "petty_cash_threshold", precision = 19, scale = 2)
    private BigDecimal pettyCashThreshold = BigDecimal.ZERO;

    @Column(name = "petty_cash_request_recipients", columnDefinition = "TEXT")
    private String pettyCashRequestRecipients;

    private Boolean emailNotificationsEnabled;
    private Boolean smsNotificationsEnabled;

    private Double taxRate = 0.15;
    private Double socialSecurityRate = 0.05;
    private Double pensionRate = 0.07;

    @Column(name = "direct_purchase_accountant_recipients", columnDefinition = "TEXT")
    private String accountantRecipients;

    @Column(name = "direct_purchase_reviewer_recipients", columnDefinition = "TEXT")
    private String reviewerRecipients;

    @Column(name = "direct_purchase_finance_recipients", columnDefinition = "TEXT")
    private String financeRecipients;

    @Column(name = "direct_purchase_requester_notification_enabled")
    private Boolean requesterNotificationEnabled = true;

    @Column(name = "direct_purchase_accountant_notification_enabled")
    private Boolean accountantNotificationEnabled = true;

    @Column(name = "direct_purchase_reviewer_notification_enabled")
    private Boolean reviewerNotificationEnabled = true;

    @Column(name = "direct_purchase_finance_notification_enabled")
    private Boolean financeNotificationEnabled = true;



    @Lob
    @Column(name = "employee_categories", columnDefinition = "LONGTEXT")
    private String employeeCategories;

    @Column(columnDefinition = "TEXT")
    private String inventoryRequestRecipients;

    @Column(columnDefinition = "TEXT")
    private String inventoryPlannerRecipients;

    @Column(columnDefinition = "TEXT")
    private String inventoryProcurementRecipients;

    @Column(columnDefinition = "TEXT")
    private String inventoryStoreRecipients;

    @Column(columnDefinition = "TEXT")
    private String inventoryStatusRecipients;

    @Column(name = "ppe_request_recipients", columnDefinition = "TEXT")
    private String ppeRequestRecipients;

    @Column(name = "ppe_procurement_recipients", columnDefinition = "TEXT")
    private String ppeProcurementRecipients;

    @Column(name = "ppe_store_recipients", columnDefinition = "TEXT")
    private String ppeStoreRecipients;

    @Column(name = "ppe_status_recipients", columnDefinition = "TEXT")
    private String ppeStatusRecipients;

    @Column(name = "ppe_requester_notification_enabled")
    private Boolean ppeRequesterNotificationEnabled = true;

    @Column(name = "ppe_procurement_notification_enabled")
    private Boolean ppeProcurementNotificationEnabled = true;

    @Column(name = "ppe_store_notification_enabled")
    private Boolean ppeStoreNotificationEnabled = true;

    @Column(name = "ppe_status_notification_enabled")
    private Boolean ppeStatusNotificationEnabled = true;

    private Boolean inventoryRequesterNotificationEnabled = true;
    private Boolean inventoryPlannerNotificationEnabled = true;
    private Boolean inventoryProcurementNotificationEnabled = true;
    private Boolean inventoryStoreNotificationEnabled = true;
    private Boolean inventoryStatusNotificationEnabled = true;

    @Transient
    @JsonIgnore
    public Integer[] getWeekendDaysArray() {
        if (weekendDays == null || weekendDays.trim().isEmpty()) {
            return new Integer[]{0, 6};
        }
        try {
            String[] days = weekendDays.split(",");
            Integer[] result = new Integer[days.length];
            for (int i = 0; i < days.length; i++) {
                result[i] = Integer.parseInt(days[i].trim());
            }
            return result;
        } catch (Exception e) {
            return new Integer[]{0, 6};
        }
    }

    @JsonIgnore
    public void setWeekendDaysArray(Integer[] days) {
        if (days == null || days.length == 0) {
            this.weekendDays = "0,6";
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < days.length; i++) {
                if (i > 0) sb.append(",");
                sb.append(days[i]);
            }
            this.weekendDays = sb.toString();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getWeekendDays() { return weekendDays; }
    public void setWeekendDays(String weekendDays) { this.weekendDays = weekendDays; }
    public Double getDefaultOvertimeMultiplier() { return defaultOvertimeMultiplier; }
    public void setDefaultOvertimeMultiplier(Double defaultOvertimeMultiplier) { this.defaultOvertimeMultiplier = defaultOvertimeMultiplier; }
    public Double getSundayOvertimeMultiplier() { return sundayOvertimeMultiplier; }
    public void setSundayOvertimeMultiplier(Double sundayOvertimeMultiplier) { this.sundayOvertimeMultiplier = sundayOvertimeMultiplier; }
    public Double getHolidayOvertimeMultiplier() { return holidayOvertimeMultiplier; }
    public void setHolidayOvertimeMultiplier(Double holidayOvertimeMultiplier) { this.holidayOvertimeMultiplier = holidayOvertimeMultiplier; }
    public Boolean getEnableTimeAndHalfAfter8Hours() { return enableTimeAndHalfAfter8Hours; }
    public void setEnableTimeAndHalfAfter8Hours(Boolean enableTimeAndHalfAfter8Hours) { this.enableTimeAndHalfAfter8Hours = enableTimeAndHalfAfter8Hours; }
    public Double getTimeAndHalfMultiplier() { return timeAndHalfMultiplier; }
    public void setTimeAndHalfMultiplier(Double timeAndHalfMultiplier) { this.timeAndHalfMultiplier = timeAndHalfMultiplier; }
    public Double getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(Double hourlyRate) { this.hourlyRate = hourlyRate; }
    public Double getOvertimeHourlyRate() { return overtimeHourlyRate; }
    public void setOvertimeHourlyRate(Double overtimeHourlyRate) { this.overtimeHourlyRate = overtimeHourlyRate; }
    public Integer getStandardWorkHours() { return standardWorkHours; }
    public void setStandardWorkHours(Integer standardWorkHours) { this.standardWorkHours = standardWorkHours; }
    public Boolean getDoubleTimeOnSunday() { return doubleTimeOnSunday; }
    public void setDoubleTimeOnSunday(Boolean doubleTimeOnSunday) { this.doubleTimeOnSunday = doubleTimeOnSunday; }
    public Boolean getTimeAndHalfAfter8Hours() { return timeAndHalfAfter8Hours; }
    public void setTimeAndHalfAfter8Hours(Boolean timeAndHalfAfter8Hours) { this.timeAndHalfAfter8Hours = timeAndHalfAfter8Hours; }
    public Double getWeekendRate() { return weekendRate; }
    public void setWeekendRate(Double weekendRate) { this.weekendRate = weekendRate; }
    public Double getHolidayRate() { return holidayRate; }
    public void setHolidayRate(Double holidayRate) { this.holidayRate = holidayRate; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getCompanyAddress() { return companyAddress; }
    public void setCompanyAddress(String companyAddress) { this.companyAddress = companyAddress; }
    public String getCompanyPhone() { return companyPhone; }
    public void setCompanyPhone(String companyPhone) { this.companyPhone = companyPhone; }
    public String getCompanyEmail() { return companyEmail; }
    public void setCompanyEmail(String companyEmail) { this.companyEmail = companyEmail; }
    public Double getTaxRate() { return taxRate; }
    public void setTaxRate(Double taxRate) { this.taxRate = taxRate; }
    public Double getSocialSecurityRate() { return socialSecurityRate; }
    public void setSocialSecurityRate(Double socialSecurityRate) { this.socialSecurityRate = socialSecurityRate; }
    public Double getPensionRate() { return pensionRate; }
    public void setPensionRate(Double pensionRate) { this.pensionRate = pensionRate; }
    public String getEmployeeCategories() { return employeeCategories; }
    public void setEmployeeCategories(String employeeCategories) { this.employeeCategories = employeeCategories; }
    public String getLateArrivalTime() { return lateArrivalTime; }
    public void setLateArrivalTime(String lateArrivalTime) { this.lateArrivalTime = lateArrivalTime; }
    public String getLoanRequestRecipients() { return loanRequestRecipients; }
    public void setLoanRequestRecipients(String loanRequestRecipients) { this.loanRequestRecipients = loanRequestRecipients; }
    public String getLoanApprovalRecipients() { return loanApprovalRecipients; }
    public void setLoanApprovalRecipients(String loanApprovalRecipients) { this.loanApprovalRecipients = loanApprovalRecipients; }
    public String getLoanRejectionRecipients() { return loanRejectionRecipients; }
    public void setLoanRejectionRecipients(String loanRejectionRecipients) { this.loanRejectionRecipients = loanRejectionRecipients; }
    public String getLeaveRequestRecipients() { return leaveRequestRecipients; }
    public void setLeaveRequestRecipients(String leaveRequestRecipients) { this.leaveRequestRecipients = leaveRequestRecipients; }
    public String getAttendanceAlertRecipients() { return attendanceAlertRecipients; }
    public void setAttendanceAlertRecipients(String attendanceAlertRecipients) { this.attendanceAlertRecipients = attendanceAlertRecipients; }
    public String getLateArrivalRecipients() { return lateArrivalRecipients; }
    public void setLateArrivalRecipients(String lateArrivalRecipients) { this.lateArrivalRecipients = lateArrivalRecipients; }
    public String getOvertimeRequestRecipients() { return overtimeRequestRecipients; }
    public void setOvertimeRequestRecipients(String overtimeRequestRecipients) { this.overtimeRequestRecipients = overtimeRequestRecipients; }
    public String getAttendanceValidationRecipients() { return attendanceValidationRecipients; }
    public void setAttendanceValidationRecipients(String attendanceValidationRecipients) { this.attendanceValidationRecipients = attendanceValidationRecipients; }
    public String getLeaveApprovalRecipients() { return leaveApprovalRecipients; }
    public void setLeaveApprovalRecipients(String leaveApprovalRecipients) { this.leaveApprovalRecipients = leaveApprovalRecipients; }
    public String getAccountRequestRecipients() { return accountRequestRecipients; }
    public void setAccountRequestRecipients(String accountRequestRecipients) { this.accountRequestRecipients = accountRequestRecipients; }
    public String getSupportRequestRecipients() { return supportRequestRecipients; }
    public void setSupportRequestRecipients(String supportRequestRecipients) { this.supportRequestRecipients = supportRequestRecipients; }
    public BigDecimal getPettyCashThreshold() { return pettyCashThreshold; }
    public void setPettyCashThreshold(BigDecimal pettyCashThreshold) { this.pettyCashThreshold = pettyCashThreshold; }
    public String getPettyCashRequestRecipients() { return pettyCashRequestRecipients; }
    public void setPettyCashRequestRecipients(String pettyCashRequestRecipients) { this.pettyCashRequestRecipients = pettyCashRequestRecipients; }
    public Boolean getEmailNotificationsEnabled() { return emailNotificationsEnabled; }
    public void setEmailNotificationsEnabled(Boolean emailNotificationsEnabled) { this.emailNotificationsEnabled = emailNotificationsEnabled; }
    public Boolean getSmsNotificationsEnabled() { return smsNotificationsEnabled; }
    public void setSmsNotificationsEnabled(Boolean smsNotificationsEnabled) { this.smsNotificationsEnabled = smsNotificationsEnabled; }
    public Double getWithholdingTaxRate() { return withholdingTaxRate; }
    public void setWithholdingTaxRate(Double withholdingTaxRate) { this.withholdingTaxRate = withholdingTaxRate; }
    public Boolean getWithholdingTaxEnabled() { return withholdingTaxEnabled; }
    public void setWithholdingTaxEnabled(Boolean withholdingTaxEnabled) { this.withholdingTaxEnabled = withholdingTaxEnabled; }
    public String getBillingReminderEmails() { return billingReminderEmails; }
    public void setBillingReminderEmails(String billingReminderEmails) { this.billingReminderEmails = billingReminderEmails; }
    public Boolean getBillingReminderEnabled() { return billingReminderEnabled; }
    public void setBillingReminderEnabled(Boolean billingReminderEnabled) { this.billingReminderEnabled = billingReminderEnabled; }
}
