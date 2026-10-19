package com.example.eac_backend_project.dto;

import java.util.List;

public class EmailConfigurationDTO {
    private List<String> leaveRequestRecipients;
    private List<String> attendanceAlertRecipients;
    private List<String> lateArrivalRecipients;
    private List<String> overtimeRequestRecipients;
    private List<String> attendanceValidationRecipients;
    private List<String> leaveApprovalRecipients;
    private List<String> accountRequestRecipients;
    private List<String> supportRequestRecipients;
    private List<String> pettyCashRequestRecipients;

    // Constructors
    public EmailConfigurationDTO() {}

    // Getters and Setters
    public List<String> getLeaveRequestRecipients() {
        return leaveRequestRecipients;
    }

    public void setLeaveRequestRecipients(List<String> leaveRequestRecipients) {
        this.leaveRequestRecipients = leaveRequestRecipients;
    }

    public List<String> getAttendanceAlertRecipients() {
        return attendanceAlertRecipients;
    }

    public void setAttendanceAlertRecipients(List<String> attendanceAlertRecipients) {
        this.attendanceAlertRecipients = attendanceAlertRecipients;
    }

    public List<String> getLateArrivalRecipients() {
        return lateArrivalRecipients;
    }

    public void setLateArrivalRecipients(List<String> lateArrivalRecipients) {
        this.lateArrivalRecipients = lateArrivalRecipients;
    }

    public List<String> getOvertimeRequestRecipients() {
        return overtimeRequestRecipients;
    }

    public void setOvertimeRequestRecipients(List<String> overtimeRequestRecipients) {
        this.overtimeRequestRecipients = overtimeRequestRecipients;
    }

    public List<String> getAttendanceValidationRecipients() {
        return attendanceValidationRecipients;
    }

    public void setAttendanceValidationRecipients(List<String> attendanceValidationRecipients) {
        this.attendanceValidationRecipients = attendanceValidationRecipients;
    }

    public List<String> getLeaveApprovalRecipients() {
        return leaveApprovalRecipients;
    }

    public void setLeaveApprovalRecipients(List<String> leaveApprovalRecipients) {
        this.leaveApprovalRecipients = leaveApprovalRecipients;
    }

    public List<String> getAccountRequestRecipients() { return accountRequestRecipients; }
    public void setAccountRequestRecipients(List<String> accountRequestRecipients) { this.accountRequestRecipients = accountRequestRecipients; }
    public List<String> getSupportRequestRecipients() { return supportRequestRecipients; }
    public void setSupportRequestRecipients(List<String> supportRequestRecipients) { this.supportRequestRecipients = supportRequestRecipients; }
    public List<String> getPettyCashRequestRecipients() { return pettyCashRequestRecipients; }
    public void setPettyCashRequestRecipients(List<String> pettyCashRequestRecipients) { this.pettyCashRequestRecipients = pettyCashRequestRecipients; }
}
