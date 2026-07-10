package com.example.eac_backend_project.dto;

import lombok.Data;

import java.time.LocalTime;
import java.util.List;

@Data
public class SystemSettingsDTO {
    private Double hourlyRate;
    private Double overtimeHourlyRate;
    private Integer standardWorkHours;
    private Boolean doubleTimeOnSunday;
    private Boolean timeAndHalfAfter8Hours;
    private Double weekendRate;
    private Double holidayRate;
    private List<Integer> weekendDays;
    private Boolean withholdingTaxEnabled;
    private Double withholdingTaxRate;
    private String companyName;
    private String companyEmail;
    private String companyPhone;
    private String companyAddress;
    private Object employeeCategories;
    private String billingReminderEmails;
    private Boolean billingReminderEnabled;
    private String lateArrivalTime;
    private String loanConsolidationRecipients;
    private String leaveRequestRecipients;
    private String attendanceAlertRecipients;
    private String lateArrivalRecipients;
    private String overtimeRequestRecipients;
    private String attendanceValidationRecipients;
    private String leaveApprovalRecipients;

    private Double defaultOvertimeMultiplier;
    private Double sundayOvertimeMultiplier;
    private Double holidayOvertimeMultiplier;
    private Boolean enableTimeAndHalfAfter8Hours;
    private Double timeAndHalfMultiplier;

    private String loanRequestRecipients;
    private String loanApprovalRecipients;
    private String loanRejectionRecipients;

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
    public List<Integer> getWeekendDays() { return weekendDays; }
    public void setWeekendDays(List<Integer> weekendDays) { this.weekendDays = weekendDays; }
    public Boolean getWithholdingTaxEnabled() { return withholdingTaxEnabled; }
    public void setWithholdingTaxEnabled(Boolean withholdingTaxEnabled) { this.withholdingTaxEnabled = withholdingTaxEnabled; }
    public Double getWithholdingTaxRate() { return withholdingTaxRate; }
    public void setWithholdingTaxRate(Double withholdingTaxRate) { this.withholdingTaxRate = withholdingTaxRate; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getCompanyEmail() { return companyEmail; }
    public void setCompanyEmail(String companyEmail) { this.companyEmail = companyEmail; }
    public String getCompanyPhone() { return companyPhone; }
    public void setCompanyPhone(String companyPhone) { this.companyPhone = companyPhone; }
    public String getCompanyAddress() { return companyAddress; }
    public void setCompanyAddress(String companyAddress) { this.companyAddress = companyAddress; }
    public Object getEmployeeCategories() { return employeeCategories; }
    public void setEmployeeCategories(Object employeeCategories) { this.employeeCategories = employeeCategories; }
    public String getBillingReminderEmails() { return billingReminderEmails; }
    public void setBillingReminderEmails(String billingReminderEmails) { this.billingReminderEmails = billingReminderEmails; }
    public Boolean getBillingReminderEnabled() { return billingReminderEnabled; }
    public void setBillingReminderEnabled(Boolean billingReminderEnabled) { this.billingReminderEnabled = billingReminderEnabled; }
    public String getLateArrivalTime() { return lateArrivalTime; }
    public void setLateArrivalTime(String lateArrivalTime) { this.lateArrivalTime = lateArrivalTime; }
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
    public String getLoanRequestRecipients() { return loanRequestRecipients; }
    public void setLoanRequestRecipients(String loanRequestRecipients) { this.loanRequestRecipients = loanRequestRecipients; }
    public String getLoanApprovalRecipients() { return loanApprovalRecipients; }
    public void setLoanApprovalRecipients(String loanApprovalRecipients) { this.loanApprovalRecipients = loanApprovalRecipients; }
    public String getLoanRejectionRecipients() { return loanRejectionRecipients; }
    public void setLoanRejectionRecipients(String loanRejectionRecipients) { this.loanRejectionRecipients = loanRejectionRecipients; }
}