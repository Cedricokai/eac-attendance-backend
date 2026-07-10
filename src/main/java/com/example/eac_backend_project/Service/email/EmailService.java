package com.example.eac_backend_project.Service.email;

import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.attendance.Job;
import com.example.eac_backend_project.entity.attendance.Leave;
import com.example.eac_backend_project.entity.attendance.LoanRequest;

import java.util.List;

public interface EmailService {
    void sendLeaveRequestNotification(Leave leave);
    void sendSupervisorApprovalNotification(Leave leave, String feedback);
    void sendPlannerApprovalNotification(Leave leave, String feedback);
    void sendHrApprovalNotification(Leave leave, String feedback);
    void sendLeaveRejectionNotification(Leave leave, String role, String feedback);
    void sendLeaveStatusUpdate(Leave leave);
    void sendHtmlEmailWithTemplate(String to, String subject, String templateName, Leave leave, Employee employee);
    void sendCustomEmail(String to, String subject, String content);
    void sendBillingReminderEmail(String to, String subject, Job job, int daysToDue);
    void sendBillingEnlistedEmail(String to, Job job);
    void sendEmailWithExcelAttachment(String to, String subject, String htmlBody, String fileName, byte[] fileBytes);
    void sendPasswordResetEmail(String to, String resetLink, String userName);
    void sendLeaveRequestNotificationToAll(Leave leave);
    void sendAttendanceAlertNotification(String subject, String message, List<String> employeeNames);
    void sendLateArrivalNotification(Employee employee, String checkInTime, String workStartTime);
    void sendOvertimeRequestNotification(Employee employee, String date, double overtimeHours);
    void sendAttendanceValidationNotification(int recordCount, String period);

    void sendLoanRequestNotificationToAll(LoanRequest loan);
    void sendLoanRequestSubmittedConfirmation(LoanRequest loan);
    void sendLoanApprovalNotification(LoanRequest loan, String feedback);
    void sendLoanRejectionNotification(LoanRequest loan, String feedback);
    void sendLoanConsolidationNotification(LoanRequest consolidatedLoan, List<LoanRequest> originalLoans);
}