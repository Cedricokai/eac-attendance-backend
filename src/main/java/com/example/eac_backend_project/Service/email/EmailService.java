package com.example.eac_backend_project.Service.email;

import com.example.eac_backend_project.dto.DirectPurchaseResponseDTO;
import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.attendance.Job;
import com.example.eac_backend_project.entity.attendance.Leave;
import com.example.eac_backend_project.entity.attendance.LoanRequest;
import com.example.eac_backend_project.entity.inventory.InventoryRequest;
import com.example.eac_backend_project.entity.attendance.Overtime;
import com.example.eac_backend_project.entity.inventory.User;
import com.example.eac_backend_project.entity.inventory.PurchaseOrder;
import com.example.eac_backend_project.dto.AttendanceRequestResponseDTO;

import java.util.List;

public interface EmailService {

    // ========== LEAVE NOTIFICATIONS ==========
    void sendLeaveRequestNotification(Leave leave);
    void sendLeaveRequestToSupervisor(Leave leave, String supervisorEmail, String supervisorName);
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
    void sendOvertimeRequestToSupervisor(Overtime overtime, String supervisorEmail, String supervisorName, User requestedBy);
    void sendOvertimeRequestToConfiguredRecipients(Overtime overtime, User requestedBy);
    void sendOvertimeRequestsToSupervisor(List<Overtime> overtimes, String supervisorEmail, String supervisorName, User requestedBy);
    void sendOvertimeRequestsToConfiguredRecipients(List<Overtime> overtimes, User requestedBy);
    void sendAttendanceValidationNotification(int recordCount, String period);
    void sendAttendanceRequestReviewNotification(List<AttendanceRequestResponseDTO> requests, String submittedBy);
    void sendBulkApprovalNotification(Leave leave, String feedback);

    // ========== LOAN NOTIFICATIONS ==========
    void sendLoanRequestNotificationToAll(LoanRequest loan);
    void sendLoanRequestSubmittedConfirmation(LoanRequest loan);
    void sendLoanApprovalNotification(LoanRequest loan, String feedback);
    void sendLoanRejectionNotification(LoanRequest loan, String feedback);
    void sendLoanConsolidationNotification(LoanRequest consolidatedLoan, List<LoanRequest> originalLoans);

    // ========== DIRECT PURCHASE NOTIFICATIONS ==========
    void sendDirectPurchaseRequestCreated(DirectPurchaseResponseDTO dto, String recipientEmail, String recipientRole);
    void sendDirectPurchaseStepApproved(DirectPurchaseResponseDTO dto, String recipientEmail, String recipientRole, String notes);
    void sendDirectPurchaseRejected(DirectPurchaseResponseDTO dto, String recipientEmail, String reason);
    void sendDirectPurchasePaid(DirectPurchaseResponseDTO dto, String requesterEmail, String accountantEmail, String notes);

    // ========== PROCUREMENT PURCHASE NOTIFICATIONS ==========
    void sendProcurementPurchaseCreated(PurchaseOrder purchaseOrder);
    void sendProcurementPurchaseStoreNotification(PurchaseOrder purchaseOrder, String notificationNotes);

    // ========== NORMAL INVENTORY NOTIFICATIONS ==========
    void sendInventoryRequestCreated(InventoryRequest request, String requesterEmail);
    void sendInventoryStatusChanged(InventoryRequest request, String action, String performedBy, String notes);

    // ========== PPE NOTIFICATIONS ==========
    void sendPpeRequestCreated(InventoryRequest request, String requesterEmail);
    void sendPpeStatusChanged(InventoryRequest request, String action, String performedBy, String notes);
}
