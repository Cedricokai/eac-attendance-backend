package com.example.eac_backend_project.Service.email;

import com.example.eac_backend_project.Service.settings.SettingsService;
import com.example.eac_backend_project.Service.FrontendUrlService;
import com.example.eac_backend_project.dto.DirectPurchaseResponseDTO;
import com.example.eac_backend_project.dto.AttendanceRequestResponseDTO;
import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.attendance.Job;
import com.example.eac_backend_project.entity.attendance.Leave;
import com.example.eac_backend_project.entity.attendance.LoanRequest;
import com.example.eac_backend_project.entity.attendance.Overtime;
import com.example.eac_backend_project.entity.inventory.InventoryRequest;
import com.example.eac_backend_project.entity.inventory.PurchaseOrder;
import com.example.eac_backend_project.entity.inventory.PurchaseOrderItem;
import com.example.eac_backend_project.entity.inventory.User;
import com.example.eac_backend_project.entity.inventory.RequestItem;
import com.example.eac_backend_project.entity.inventory.RequestStatus;
import com.example.eac_backend_project.entity.settings.SystemSettings;
import com.example.eac_backend_project.utils.EmailRecipientHelper;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final SettingsService settingsService;
    private final EmailRecipientHelper emailRecipientHelper;
    private final FrontendUrlService frontendUrlService;

    @Autowired
    private com.example.eac_backend_project.repository.UserRepository userRepository;

    @Value("${spring.mail.username}")
    private String smtpUsername;

    @Autowired
    public EmailServiceImpl(JavaMailSender mailSender, TemplateEngine templateEngine,
                            SettingsService settingsService, EmailRecipientHelper emailRecipientHelper,
                            FrontendUrlService frontendUrlService) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.settingsService = settingsService;
        this.emailRecipientHelper = emailRecipientHelper;
        this.frontendUrlService = frontendUrlService;
    }

    private void sendToMultipleRecipients(List<String> recipients, String subject,
                                          String templateName, Leave leave, Employee employee) {
        if (recipients == null || recipients.isEmpty()) {
            return;
        }
        for (String recipient : recipients) {
            try {
                sendHtmlEmailWithTemplate(recipient, subject, templateName, leave, employee);
            } catch (Exception e) {
                System.err.println("Failed to send email to " + recipient + ": " + e.getMessage());
            }
        }
    }

    // ==================== CORE EMAIL METHOD ====================

    @Async
    @Override
    public void sendHtmlEmailWithTemplate(String to, String subject, String templateName,
                                          Leave leave, Employee employee) {
        try {
            SystemSettings systemSettings = settingsService.getSystemSettings();
            String companyName = systemSettings.getCompanyName();
            String companyEmail = systemSettings.getCompanyEmail();
            String companyPhone = systemSettings.getCompanyPhone();
            String companyAddress = systemSettings.getCompanyAddress();

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setTo(to);
            helper.setSubject(subject);

            if (companyName != null && !companyName.trim().isEmpty()) {
                try {
                    helper.setFrom(smtpUsername, companyName);
                } catch (UnsupportedEncodingException ex) {
                    helper.setFrom(smtpUsername);
                }
            } else {
                helper.setFrom(smtpUsername);
            }

            if (companyEmail != null && !companyEmail.trim().isEmpty()) {
                helper.setReplyTo(companyEmail);
            }

            Map<String, Object> variables = new HashMap<>();
            variables.put("baseUrl", frontendUrlService.getBaseUrl() + "/#");
            variables.put("companyName", companyName);
            variables.put("companyEmail", companyEmail);
            variables.put("companyPhone", companyPhone);
            variables.put("companyAddress", companyAddress);
            variables.put("leave", leave);

            if (employee != null) {
                String employeeName = (employee.getFirstName() == null ? "" : employee.getFirstName()) + " " +
                        (employee.getLastName() == null ? "" : employee.getLastName());
                variables.put("employeeName", employeeName.trim());
            }

            if (leave != null) {
                variables.put("leaveType", leave.getLeaveType());
                variables.put("startDate", leave.getStartDate());
                variables.put("endDate", leave.getEndDate());
                variables.put("reason", leave.getReason());
                variables.put("status", leave.getStatus());
            }

            Context context = new Context();
            context.setVariables(variables);

            String html = templateEngine.process(templateName, context);
            helper.setText(html, true);

            mailSender.send(message);

        } catch (MessagingException ex) {
            throw new RuntimeException("Failed to send email: " + ex.getMessage(), ex);
        }
    }

    // ==================== LEAVE REQUEST NOTIFICATIONS ====================

    @Override
    public void sendLeaveRequestNotification(Leave leave) {
        sendHtmlEmailWithTemplate(
                leave.getEmployee().getEmail(),
                "Leave Request Submitted - " + leave.getLeaveType(),
                "leave-request-submitted",
                leave,
                leave.getEmployee()
        );
    }

    @Override
    public void sendLeaveRequestNotificationToAll(Leave leave) {
        SystemSettings settings = settingsService.getSystemSettings();
        List<String> recipients = emailRecipientHelper.parseEmailRecipients(
                settings.getLeaveRequestRecipients()
        );

        if (!recipients.isEmpty()) {
            for (String recipient : recipients) {
                try {
                    sendHtmlEmailWithTemplateForApprover(recipient,
                            "ACTION REQUIRED: Leave Request from " + leave.getEmployee().getFirstName() + " " + leave.getEmployee().getLastName(),
                            "leave-request-approver-notification",
                            leave,
                            leave.getEmployee(),
                            null);
                } catch (Exception e) {
                    System.err.println("Failed to send email to " + recipient + ": " + e.getMessage());
                }
            }
        }
    }

    // ==================== LEAVE APPROVAL NOTIFICATIONS ====================

    @Override
    public void sendSupervisorApprovalNotification(Leave leave, String feedback) {
        sendLeaveDecision(leave, "Approved by supervisor", feedback);
    }

    @Override
    public void sendPlannerApprovalNotification(Leave leave, String feedback) {
        sendLeaveDecision(leave, "Approved by planner", feedback);
    }

    @Override
    public void sendHrApprovalNotification(Leave leave, String feedback) {
        sendLeaveDecision(leave, "Approved by HR", feedback);
    }

    @Override
    public void sendBulkApprovalNotification(Leave leave, String feedback) {
        sendLeaveDecision(leave, "Approved", feedback);
    }

    @Override
    public void sendLeaveRejectionNotification(Leave leave, String role, String feedback) {
        sendLeaveDecision(leave, "Rejected" + (role == null || role.isBlank() ? "" : " by " + role), feedback);
    }

    private String leaveDecisionActor() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            return "Not recorded";
        }
        String username = authentication.getName();
        try {
            if (userRepository != null) {
                return userRepository.findByUserName(username)
                        .map(User::getName)
                        .filter(name -> !name.isBlank())
                        .orElse(username);
            }
        } catch (Exception e) {
            System.err.println("Failed to resolve leave decision actor: " + e.getMessage());
        }
        return username;
    }

    private void sendLeaveDecision(Leave leave, String decision, String feedback) {
        String decisionActor = leaveDecisionActor();
        Employee employee = leave.getEmployee();
        if (employee == null) return;
        Set<String> recipients = new LinkedHashSet<>();
        String employeeEmail = employee.getEmail() == null ? "" : employee.getEmail().trim();
        if (!employeeEmail.isEmpty()) recipients.add(employeeEmail.toLowerCase(java.util.Locale.ROOT));
        try {
            for (String recipient : emailRecipientHelper.parseEmailRecipients(
                    settingsService.getSystemSettings().getLeaveApprovalRecipients())) {
                if (recipient != null && !recipient.isBlank()) {
                    recipients.add(recipient.trim().toLowerCase(java.util.Locale.ROOT));
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load leave decision recipients: " + e.getMessage());
        }
        for (String recipient : recipients) {
            try {
                Map<String, Object> data = new HashMap<>();
                data.put("employeeNotice", recipient.equalsIgnoreCase(employeeEmail));
                data.put("decision", decision);
                data.put("decisionActor", decisionActor);
                data.put("feedback", feedback);
                data.put("finalApproval", "Approved".equalsIgnoreCase(leave.getStatus()));
                data.put("rejected", "Rejected".equalsIgnoreCase(leave.getStatus()));
                data.put("status", leave.getStatus());
                sendHtmlEmailWithTemplateForApprover(recipient,
                        "Leave Request - " + decision + " - " + employee.getFirstName() + " " + employee.getLastName(),
                        "leave-decision-notification", leave, employee, data);
            } catch (Exception e) {
                // A delivery failure must not prevent other recipients from being notified.
                System.err.println("Failed to send leave decision email to " + recipient + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void sendLeaveStatusUpdate(Leave leave) {
        sendHtmlEmailWithTemplate(
                leave.getEmployee().getEmail(),
                "Leave Status Update",
                "leave-status-update",
                leave,
                leave.getEmployee()
        );
    }

    private void sendHtmlEmailWithTemplateForApprover(String to, String subject, String templateName,
                                                      Leave leave, Employee employee, Map<String, Object> additionalData) {
        try {
            SystemSettings systemSettings = settingsService.getSystemSettings();
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setTo(to);
            helper.setSubject(subject);

            String companyName = systemSettings.getCompanyName();
            if (companyName != null && !companyName.trim().isEmpty()) {
                try {
                    helper.setFrom(smtpUsername, companyName);
                } catch (UnsupportedEncodingException ex) {
                    helper.setFrom(smtpUsername);
                }
            } else {
                helper.setFrom(smtpUsername);
            }

            if (systemSettings.getCompanyEmail() != null && !systemSettings.getCompanyEmail().trim().isEmpty()) {
                helper.setReplyTo(systemSettings.getCompanyEmail());
            }

            Map<String, Object> variables = new HashMap<>();
            variables.put("baseUrl", frontendUrlService.getBaseUrl() + "/#");
            variables.put("companyName", companyName);
            variables.put("companyEmail", systemSettings.getCompanyEmail());
            variables.put("companyPhone", systemSettings.getCompanyPhone());
            variables.put("companyAddress", systemSettings.getCompanyAddress());
            variables.put("leave", leave);
            variables.put("employeeName", (employee.getFirstName() + " " + employee.getLastName()).trim());
            variables.put("leaveType", leave.getLeaveType());
            variables.put("startDate", leave.getStartDate());
            variables.put("endDate", leave.getEndDate());
            variables.put("reason", leave.getReason());

            if (additionalData != null) {
                variables.putAll(additionalData);
            }

            Context context = new Context();
            context.setVariables(variables);

            String html = templateEngine.process(templateName, context);
            helper.setText(html, true);
            mailSender.send(message);

        } catch (MessagingException ex) {
            throw new RuntimeException("Failed to send email: " + ex.getMessage(), ex);
        }
    }

    // ==================== DIRECT PURCHASE NOTIFICATIONS ====================

    private void sendDirectPurchaseEmailWithTemplate(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            SystemSettings systemSettings = settingsService.getSystemSettings();
            String companyName = systemSettings.getCompanyName();
            String companyEmail = systemSettings.getCompanyEmail();
            String companyPhone = systemSettings.getCompanyPhone();
            String companyAddress = systemSettings.getCompanyAddress();

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setTo(to);
            helper.setSubject(subject);

            if (companyName != null && !companyName.trim().isEmpty()) {
                try {
                    helper.setFrom(smtpUsername, companyName);
                } catch (UnsupportedEncodingException ex) {
                    helper.setFrom(smtpUsername);
                }
            } else {
                helper.setFrom(smtpUsername);
            }

            if (companyEmail != null && !companyEmail.trim().isEmpty()) {
                helper.setReplyTo(companyEmail);
            }

            Map<String, Object> allVariables = new HashMap<>();
            allVariables.put("baseUrl", frontendUrlService.getBaseUrl() + "/#");
            allVariables.put("companyName", companyName);
            allVariables.put("companyEmail", companyEmail);
            allVariables.put("companyPhone", companyPhone);
            allVariables.put("companyAddress", companyAddress);

            if (variables != null) {
                allVariables.putAll(variables);
            }

            Context context = new Context();
            context.setVariables(allVariables);

            String html = templateEngine.process(templateName, context);
            helper.setText(html, true);

            mailSender.send(message);

        } catch (MessagingException ex) {
            throw new RuntimeException("Failed to send direct purchase email: " + ex.getMessage(), ex);
        }
    }

    @Async
    @Override
    public void sendDirectPurchaseRequestCreated(DirectPurchaseResponseDTO dto, String recipientEmail, String recipientRole) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("request", dto);
            variables.put("recipientRole", recipientRole);
            variables.put("status", "PENDING");
            variables.put("actionRequired", true);
            variables.put("items", dto.getItems());
            variables.put("totalAmount", dto.getTotalAmount());
            variables.put("requestNumber", dto.getRequestNumber());
            variables.put("requester", dto.getRequester());
            variables.put("accountant", dto.getAccountant());
            variables.put("notes", dto.getNotes());

            String subject = "📋 New Direct Purchase Request - " + dto.getRequestNumber();
            sendDirectPurchaseEmailWithTemplate(recipientEmail, subject, "direct-purchase-request-created", variables);
            System.out.println("✅ Direct Purchase request email sent to: " + recipientEmail);
        } catch (Exception e) {
            System.err.println("❌ Failed to send Direct Purchase request email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Async
    @Override
    public void sendDirectPurchaseStepApproved(DirectPurchaseResponseDTO dto, String recipientEmail, String recipientRole, String notes) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("request", dto);
            variables.put("recipientRole", recipientRole);
            variables.put("status", dto.getStatus().name());
            variables.put("notes", notes != null ? notes : "");
            variables.put("actionRequired", true);
            variables.put("items", dto.getItems());
            variables.put("totalAmount", dto.getTotalAmount());
            variables.put("requestNumber", dto.getRequestNumber());
            variables.put("requester", dto.getRequester());
            variables.put("accountant", dto.getAccountant());

            String subject = "✅ Direct Purchase Approved - " + dto.getRequestNumber() + " (" + recipientRole + " Action Required)";
            sendDirectPurchaseEmailWithTemplate(recipientEmail, subject, "direct-purchase-step-approved", variables);
            System.out.println("✅ Direct Purchase step approval email sent to: " + recipientEmail);
        } catch (Exception e) {
            System.err.println("❌ Failed to send Direct Purchase step approval email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Async
    @Override
    public void sendDirectPurchaseRejected(DirectPurchaseResponseDTO dto, String recipientEmail, String reason) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("request", dto);
            variables.put("reason", reason != null ? reason : "No reason provided");
            variables.put("status", "REJECTED");
            variables.put("items", dto.getItems());
            variables.put("totalAmount", dto.getTotalAmount());
            variables.put("requestNumber", dto.getRequestNumber());
            variables.put("requester", dto.getRequester());

            String subject = "❌ Direct Purchase Rejected - " + dto.getRequestNumber();
            sendDirectPurchaseEmailWithTemplate(recipientEmail, subject, "direct-purchase-rejected", variables);
            System.out.println("✅ Direct Purchase rejection email sent to: " + recipientEmail);
        } catch (Exception e) {
            System.err.println("❌ Failed to send Direct Purchase rejection email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Async
    @Override
    public void sendDirectPurchasePaid(DirectPurchaseResponseDTO dto, String requesterEmail, String accountantEmail, String notes) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("request", dto);
            variables.put("notes", notes != null ? notes : "");
            variables.put("status", "PAID");
            variables.put("paidDate", LocalDate.now().toString());
            variables.put("items", dto.getItems());
            variables.put("totalAmount", dto.getTotalAmount());
            variables.put("requestNumber", dto.getRequestNumber());
            variables.put("requester", dto.getRequester());
            variables.put("accountant", dto.getAccountant());

            // Send to Requester
            String subjectRequester = "✅ Direct Purchase Paid - " + dto.getRequestNumber();
            sendDirectPurchaseEmailWithTemplate(requesterEmail, subjectRequester, "direct-purchase-paid-requester", variables);
            System.out.println("✅ Direct Purchase paid email sent to Requester: " + requesterEmail);

            // Send to Accountant (CC)
            if (accountantEmail != null && !accountantEmail.isEmpty()) {
                String subjectAccountant = "✅ Direct Purchase Paid - " + dto.getRequestNumber() + " (Notification)";
                sendDirectPurchaseEmailWithTemplate(accountantEmail, subjectAccountant, "direct-purchase-paid-accountant", variables);
                System.out.println("✅ Direct Purchase paid email sent to Accountant: " + accountantEmail);
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to send Direct Purchase paid email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== ATTENDANCE NOTIFICATIONS ====================

    @Override
    public void sendAttendanceAlertNotification(String subject, String message, List<String> employeeNames) {
        SystemSettings settings = settingsService.getSystemSettings();
        List<String> recipients = emailRecipientHelper.parseEmailRecipients(settings.getAttendanceAlertRecipients());
        if (recipients.isEmpty()) return;

        for (String recipient : recipients) {
            try {
                SystemSettings systemSettings = settingsService.getSystemSettings();
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
                helper.setTo(recipient);
                helper.setSubject(subject);

                String companyName = systemSettings.getCompanyName();
                if (companyName != null && !companyName.trim().isEmpty()) {
                    try {
                        helper.setFrom(smtpUsername, companyName);
                    } catch (UnsupportedEncodingException ex) {
                        helper.setFrom(smtpUsername);
                    }
                } else {
                    helper.setFrom(smtpUsername);
                }

                StringBuilder htmlContent = new StringBuilder("<html><body>")
                        .append("<h2>").append(subject).append("</h2>")
                        .append("<p>").append(message).append("</p>")
                        .append("<h3>Affected Employees:</h3><ul>");

                for (String name : employeeNames) {
                    htmlContent.append("<li>").append(name).append("</li>");
                }

                htmlContent.append("</ul><br/><hr/>")
                        .append("<footer style='font-size:12px; color:gray;'>")
                        .append("This is an automated notification from your HR System.</footer>")
                        .append("</body></html>");

                helper.setText(htmlContent.toString(), true);
                mailSender.send(mimeMessage);
                System.out.println("✅ Attendance alert sent to: " + recipient);
            } catch (Exception e) {
                System.err.println("Failed to send attendance alert to " + recipient + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void sendLateArrivalNotification(Employee employee, String checkInTime, String workStartTime) {
        SystemSettings settings = settingsService.getSystemSettings();
        List<String> recipients = emailRecipientHelper.parseEmailRecipients(settings.getLateArrivalRecipients());
        if (recipients.isEmpty()) return;

        String subject = "Late Arrival Alert - " + employee.getFirstName() + " " + employee.getLastName();
        String message = String.format(
                "Employee %s %s checked in at %s (scheduled start: %s) on %s",
                employee.getFirstName(), employee.getLastName(), checkInTime, workStartTime, LocalDate.now()
        );

        for (String recipient : recipients) {
            try {
                sendCustomEmail(recipient, subject, message);
                System.out.println("✅ Late arrival notification sent to: " + recipient);
            } catch (Exception e) {
                System.err.println("Failed to send late arrival notification to " + recipient + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void sendOvertimeRequestNotification(Employee employee, String date, double overtimeHours) {
        SystemSettings settings = settingsService.getSystemSettings();
        List<String> recipients = emailRecipientHelper.parseEmailRecipients(settings.getOvertimeRequestRecipients());
        if (recipients.isEmpty()) return;

        String subject = "Overtime Request - " + employee.getFirstName() + " " + employee.getLastName();
        String message = String.format(
                "Employee %s %s has recorded %.2f hours of overtime on %s. Please review and approve.",
                employee.getFirstName(), employee.getLastName(), overtimeHours, date
        );

        for (String recipient : recipients) {
            try {
                sendCustomEmail(recipient, subject, message);
                System.out.println("✅ Overtime request notification sent to: " + recipient);
            } catch (Exception e) {
                System.err.println("Failed to send overtime notification to " + recipient + ": " + e.getMessage());
            }
        }
    }

    @Async
    @Override
    public void sendOvertimeRequestToSupervisor(Overtime overtime, String supervisorEmail, String supervisorName, User requestedBy) {
        sendOvertimeRequestsToSupervisor(List.of(overtime), supervisorEmail, supervisorName, requestedBy);
    }

    @Async
    @Override
    public void sendOvertimeRequestsToSupervisor(List<Overtime> overtimes, String supervisorEmail, String supervisorName, User requestedBy) {
        if (supervisorEmail == null || supervisorEmail.isBlank()) {
            throw new IllegalArgumentException("Supervisor email is required");
        }
        if (overtimes == null || overtimes.isEmpty()) return;
        try {
            String submitterName = requestedBy != null && requestedBy.getName() != null
                    ? requestedBy.getName()
                    : "Unknown user";

            SystemSettings systemSettings = settingsService.getSystemSettings();
            Context context = new Context();
            context.setVariable("recipientName", supervisorName == null || supervisorName.isBlank() ? "Reviewer" : supervisorName);
            context.setVariable("requestedBy", submitterName);
            context.setVariable("overtimes", overtimes);
            context.setVariable("requestCount", overtimes.size());
            context.setVariable("reviewUrl", frontendUrlService.route("/overtime"));

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            helper.setTo(supervisorEmail.trim());
            helper.setSubject(overtimes.size() + " overtime request" + (overtimes.size() == 1 ? "" : "s") + " for review");
            if (systemSettings.getCompanyName() != null && !systemSettings.getCompanyName().isBlank()) {
                helper.setFrom(smtpUsername, systemSettings.getCompanyName());
            } else {
                helper.setFrom(smtpUsername);
            }
            helper.setText(templateEngine.process("overtime-review-request", context), true);
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send overtime review email: " + e.getMessage(), e);
        }
    }

    @Async
    @Override
    public void sendOvertimeRequestToConfiguredRecipients(Overtime overtime, User requestedBy) {
        sendOvertimeRequestsToConfiguredRecipients(List.of(overtime), requestedBy);
    }

    @Async
    @Override
    public void sendOvertimeRequestsToConfiguredRecipients(List<Overtime> overtimes, User requestedBy) {
        SystemSettings settings = settingsService.getSystemSettings();
        List<String> recipients = emailRecipientHelper.parseEmailRecipients(settings.getOvertimeRequestRecipients());
        for (String recipient : recipients) {
            sendOvertimeRequestsToSupervisor(overtimes, recipient, null, requestedBy);
        }
    }

    @Override
    public void sendAttendanceValidationNotification(int recordCount, String period) {
        SystemSettings settings = settingsService.getSystemSettings();
        List<String> recipients = emailRecipientHelper.parseEmailRecipients(settings.getAttendanceValidationRecipients());
        if (recipients.isEmpty()) return;

        String subject = "Attendance Validation Complete - " + period;
        String message = String.format(
                "Attendance validation has been completed for %s. Total records validated: %d. Ready for payroll processing.",
                period, recordCount
        );

        for (String recipient : recipients) {
            try {
                sendCustomEmail(recipient, subject, message);
                System.out.println("✅ Attendance validation notification sent to: " + recipient);
            } catch (Exception e) {
                System.err.println("Failed to send validation notification to " + recipient + ": " + e.getMessage());
            }
        }
    }

    @Async
    @Override
    public void sendAttendanceRequestReviewNotification(List<AttendanceRequestResponseDTO> requests, String submittedBy) {
        if (requests == null || requests.isEmpty()) return;
        SystemSettings settings = settingsService.getSystemSettings();
        List<String> recipients = emailRecipientHelper.parseEmailRecipients(settings.getAttendanceValidationRecipients());
        if (recipients.isEmpty()) {
            System.out.println("No attendance validation recipients configured; attendance request email skipped");
            return;
        }

        String subject = requests.size() + " attendance request" + (requests.size() == 1 ? "" : "s") + " awaiting verification";
        Context context = new Context();
        context.setVariable("companyName", settings.getCompanyName());
        context.setVariable("submittedBy", submittedBy == null || submittedBy.isBlank() ? "Unknown user" : submittedBy);
        context.setVariable("requests", requests);
        context.setVariable("requestCount", requests.size());
        context.setVariable("reviewUrl", frontendUrlService.route("/attendanceRequestReview"));
        String html = templateEngine.process("attendance-request-review", context);

        for (String recipient : recipients) {
            try {
                sendCustomEmail(recipient, subject, html);
            } catch (Exception exception) {
                System.err.println("Failed to send attendance request review email to " + recipient + ": " + exception.getMessage());
            }
        }
    }


    // ==================== PAYROLL NOTIFICATIONS ====================

    @Override
    public void sendCustomEmail(String to, String subject, String content) {
        try {
            SystemSettings systemSettings = settingsService.getSystemSettings();
            String companyName = systemSettings.getCompanyName();
            String companyEmail = systemSettings.getCompanyEmail();

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setTo(to);
            helper.setSubject(subject);

            if (companyName != null && !companyName.trim().isEmpty()) {
                try {
                    helper.setFrom(smtpUsername, companyName);
                } catch (UnsupportedEncodingException ex) {
                    helper.setFrom(smtpUsername);
                }
            } else {
                helper.setFrom(smtpUsername);
            }

            if (companyEmail != null && !companyEmail.trim().isEmpty()) {
                helper.setReplyTo(companyEmail);
            }

            helper.setText(content, true);
            mailSender.send(message);
            System.out.println("✅ Custom email sent to: " + to);
        } catch (MessagingException ex) {
            throw new RuntimeException("Failed to send custom email: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void sendEmailWithExcelAttachment(String to, String subject, String htmlBody, String fileName, byte[] fileBytes) {
        try {
            SystemSettings systemSettings = settingsService.getSystemSettings();
            String companyName = systemSettings.getCompanyName();
            String companyEmail = systemSettings.getCompanyEmail();

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setTo(to);
            helper.setSubject(subject);

            if (companyName != null && !companyName.trim().isEmpty()) {
                try {
                    helper.setFrom(smtpUsername, companyName);
                } catch (UnsupportedEncodingException ex) {
                    helper.setFrom(smtpUsername);
                }
            } else {
                helper.setFrom(smtpUsername);
            }

            if (companyEmail != null && !companyEmail.trim().isEmpty()) {
                helper.setReplyTo(companyEmail);
            }

            helper.setText(htmlBody, true);
            ByteArrayResource attachment = new ByteArrayResource(fileBytes);
            helper.addAttachment(fileName, attachment, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            mailSender.send(message);
            System.out.println("✅ Email with Excel attachment sent to: " + to);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to send email with Excel attachment: " + ex.getMessage(), ex);
        }
    }

    // ==================== BILLING NOTIFICATIONS ====================

    @Override
    public void sendBillingReminderEmail(String to, String subject, Job job, int daysToDue) {
        try {
            SystemSettings systemSettings = settingsService.getSystemSettings();
            String companyName = systemSettings.getCompanyName();
            String companyEmail = systemSettings.getCompanyEmail();
            String companyPhone = systemSettings.getCompanyPhone();
            String companyAddress = systemSettings.getCompanyAddress();

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setTo(to);
            helper.setSubject(subject);

            if (companyName != null && !companyName.trim().isEmpty()) {
                try {
                    helper.setFrom(smtpUsername, companyName);
                } catch (UnsupportedEncodingException ex) {
                    helper.setFrom(smtpUsername);
                }
            } else {
                helper.setFrom(smtpUsername);
            }

            if (companyEmail != null && !companyEmail.trim().isEmpty()) {
                helper.setReplyTo(companyEmail);
            }

            Map<String, Object> variables = new HashMap<>();
            variables.put("baseUrl", frontendUrlService.getBaseUrl() + "/#");
            variables.put("companyName", companyName);
            variables.put("companyEmail", companyEmail);
            variables.put("companyPhone", companyPhone);
            variables.put("companyAddress", companyAddress);
            variables.put("job", job);
            variables.put("jobName", job != null ? job.getName() : "");
            variables.put("client", job != null ? job.getClient() : "");
            variables.put("dueDate", job != null ? job.getNextBillingDate() : null);
            variables.put("daysToDue", daysToDue);
            variables.put("budget", job != null && job.getBudget() != null ? String.format("$%.2f", job.getBudget()) : "Not specified");
            variables.put("billingCycle", job != null && job.getBillingCycle() != null ? job.getBillingCycle().toString() : "N/A");

            String urgencyLevel;
            String urgencyColor;
            if (daysToDue == 0) {
                urgencyLevel = "DUE TODAY";
                urgencyColor = "#dc3545";
            } else if (daysToDue <= 2) {
                urgencyLevel = "URGENT - Due Soon";
                urgencyColor = "#fd7e14";
            } else if (daysToDue <= 5) {
                urgencyLevel = "Upcoming";
                urgencyColor = "#ffc107";
            } else {
                urgencyLevel = "Advance Notice";
                urgencyColor = "#28a745";
            }
            variables.put("urgencyLevel", urgencyLevel);
            variables.put("urgencyColor", urgencyColor);

            String formattedDueDate = job != null && job.getNextBillingDate() != null
                    ? job.getNextBillingDate().format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
                    : "Not scheduled";
            variables.put("formattedDueDate", formattedDueDate);

            Context context = new Context();
            context.setVariables(variables);

            String templateName;
            if (daysToDue == 0) {
                templateName = "billing-due-today";
            } else if (daysToDue <= 2) {
                templateName = "billing-urgent";
            } else {
                templateName = "billing-reminder";
            }

            String html = templateEngine.process(templateName, context);
            helper.setText(html, true);
            mailSender.send(message);
            System.out.println("✅ Billing reminder email sent to: " + to);
        } catch (MessagingException ex) {
            throw new RuntimeException("Failed to send billing reminder email: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void sendBillingEnlistedEmail(String to, Job job) {
        try {
            SystemSettings systemSettings = settingsService.getSystemSettings();
            String companyName = systemSettings.getCompanyName();
            String companyEmail = systemSettings.getCompanyEmail();
            String companyPhone = systemSettings.getCompanyPhone();
            String companyAddress = systemSettings.getCompanyAddress();

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            String jobName = job != null && job.getName() != null ? job.getName().trim() : "";
            String subject = "You have been enlisted for billing reminders - " + jobName;

            helper.setTo(to);
            helper.setSubject(subject);

            if (companyName != null && !companyName.trim().isEmpty()) {
                try {
                    helper.setFrom(smtpUsername, companyName);
                } catch (UnsupportedEncodingException ex) {
                    helper.setFrom(smtpUsername);
                }
            } else {
                helper.setFrom(smtpUsername);
            }

            if (companyEmail != null && !companyEmail.trim().isEmpty()) {
                helper.setReplyTo(companyEmail);
            }

            Map<String, Object> variables = new HashMap<>();
            variables.put("baseUrl", frontendUrlService.getBaseUrl() + "/#");
            variables.put("companyName", companyName);
            variables.put("companyEmail", companyEmail);
            variables.put("companyPhone", companyPhone);
            variables.put("companyAddress", companyAddress);
            variables.put("job", job);
            variables.put("jobName", job != null ? job.getName() : "");
            variables.put("client", job != null ? job.getClient() : "");
            variables.put("dueDate", job != null ? job.getNextBillingDate() : null);

            String formattedDueDate = job != null && job.getNextBillingDate() != null
                    ? job.getNextBillingDate().format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
                    : "Not scheduled";
            variables.put("formattedDueDate", formattedDueDate);
            variables.put("budget", job != null && job.getBudget() != null ? String.format("$%.2f", job.getBudget()) : "Not specified");
            variables.put("billingCycle", job != null && job.getBillingCycle() != null ? job.getBillingCycle().toString() : "N/A");

            Context context = new Context();
            context.setVariables(variables);

            String html = templateEngine.process("billing-enlisted", context);
            helper.setText(html, true);
            mailSender.send(message);
            System.out.println("✅ Billing enlisted email sent to: " + to);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send billing enlisted email: " + e.getMessage(), e);
        }
    }

    // ==================== PASSWORD RESET ====================

    @Override
    public void sendPasswordResetEmail(String to, String resetLink, String userName) {
        try {
            SystemSettings systemSettings = settingsService.getSystemSettings();
            String companyName = systemSettings.getCompanyName();
            String companyEmail = systemSettings.getCompanyEmail();
            String companyPhone = systemSettings.getCompanyPhone();
            String companyAddress = systemSettings.getCompanyAddress();

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setTo(to);
            helper.setSubject("Password Reset Request");

            if (companyName != null && !companyName.trim().isEmpty()) {
                try {
                    helper.setFrom(smtpUsername, companyName);
                } catch (UnsupportedEncodingException ex) {
                    helper.setFrom(smtpUsername);
                }
            } else {
                helper.setFrom(smtpUsername);
            }

            if (companyEmail != null && !companyEmail.trim().isEmpty()) {
                helper.setReplyTo(companyEmail);
            }

            Map<String, Object> variables = new HashMap<>();
            variables.put("companyName", companyName);
            variables.put("companyEmail", companyEmail);
            variables.put("companyPhone", companyPhone);
            variables.put("companyAddress", companyAddress);
            variables.put("resetLink", resetLink);
            variables.put("userName", userName);
            variables.put("baseUrl", frontendUrlService.getBaseUrl() + "/#");

            Context context = new Context();
            context.setVariables(variables);

            String html = templateEngine.process("password-reset", context);
            helper.setText(html, true);

            mailSender.send(message);
            System.out.println("✅ Password reset email sent to: " + to);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to send password reset email: " + ex.getMessage(), ex);
        }
    }

    // ==================== LOAN NOTIFICATIONS ====================

    private void sendLoanEmailWithTemplate(String to, String subject, String templateName,
                                           LoanRequest loan, Map<String, Object> additionalData) {
        try {
            SystemSettings systemSettings = settingsService.getSystemSettings();
            String companyName = systemSettings.getCompanyName();
            String companyEmail = systemSettings.getCompanyEmail();
            String companyPhone = systemSettings.getCompanyPhone();
            String companyAddress = systemSettings.getCompanyAddress();

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setTo(to);
            helper.setSubject(subject);

            if (companyName != null && !companyName.trim().isEmpty()) {
                try {
                    helper.setFrom(smtpUsername, companyName);
                } catch (UnsupportedEncodingException ex) {
                    helper.setFrom(smtpUsername);
                }
            } else {
                helper.setFrom(smtpUsername);
            }

            if (systemSettings.getCompanyEmail() != null && !systemSettings.getCompanyEmail().trim().isEmpty()) {
                helper.setReplyTo(systemSettings.getCompanyEmail());
            }

            Map<String, Object> variables = new HashMap<>();
            variables.put("baseUrl", frontendUrlService.getBaseUrl() + "/#");
            variables.put("companyName", companyName);
            variables.put("companyEmail", systemSettings.getCompanyEmail());
            variables.put("companyPhone", systemSettings.getCompanyPhone());
            variables.put("companyAddress", systemSettings.getCompanyAddress());
            variables.put("loan", loan);
            variables.put("loanId", loan.getId());
            variables.put("loanAmount", loan.getLoanAmount());
            variables.put("approvedAmount", loan.getApprovedAmount());
            variables.put("requestedAmount", loan.getLoanAmount());
            variables.put("repaymentPeriod", loan.getRepaymentPeriod());
            variables.put("approvedRepaymentPeriod", loan.getApprovedRepaymentPeriod());
            variables.put("monthlyPayment", loan.getMonthlyPaymentAmount());
            variables.put("approvedMonthlyPayment", loan.getApprovedMonthlyPayment());
            variables.put("remainingBalance", loan.getRemainingBalance());
            variables.put("purpose", loan.getPurpose());
            variables.put("status", loan.getStatus());
            variables.put("requestDate", loan.getRequestDate());
            variables.put("processedDate", loan.getProcessedDate());
            variables.put("processedBy", loan.getProcessedBy());

            if (loan.getEmployee() != null) {
                variables.put("employeeName", getEmployeeName(loan.getEmployee()));
                variables.put("employeeId", loan.getEmployee().getEmployeeId());
                variables.put("department", loan.getEmployee().getDepartment());
            }

            if (additionalData != null) {
                variables.putAll(additionalData);
            }

            Context context = new Context();
            context.setVariables(variables);

            String html = templateEngine.process(templateName, context);
            helper.setText(html, true);
            mailSender.send(message);

        } catch (MessagingException ex) {
            throw new RuntimeException("Failed to send loan email: " + ex.getMessage(), ex);
        }
    }

    @Async
    @Override
    public void sendLoanRequestNotificationToAll(LoanRequest loan) {
        SystemSettings settings = settingsService.getSystemSettings();
        List<String> recipients = emailRecipientHelper.parseEmailRecipients(settings.getLoanRequestRecipients());
        if (!recipients.isEmpty()) {
            for (String recipient : recipients) {
                try {
                    sendLoanEmailWithTemplate(recipient,
                            "ACTION REQUIRED: Loan Request from " + getEmployeeName(loan.getEmployee()),
                            "loan-request-approver-notification",
                            loan,
                            null);
                } catch (Exception e) {
                    System.err.println("Failed to send loan request notification to " + recipient + ": " + e.getMessage());
                }
            }
        }
    }

    @Async
    @Override
    public void sendLoanRequestSubmittedConfirmation(LoanRequest loan) {
        if (loan.getEmployee() != null && loan.getEmployee().getEmail() != null) {
            try {
                sendLoanEmailWithTemplate(loan.getEmployee().getEmail(),
                        "Loan Request Submitted - Confirmation",
                        "loan-request-submitted",
                        loan,
                        null);
            } catch (Exception e) {
                System.err.println("Failed to send loan confirmation to employee: " + e.getMessage());
            }
        }
    }

    @Async
    @Override
    public void sendLoanApprovalNotification(LoanRequest loan, String feedback) {
        if (loan.getEmployee() != null && loan.getEmployee().getEmail() != null) {
            try {
                Map<String, Object> additionalData = new HashMap<>();
                additionalData.put("feedback", feedback);
                additionalData.put("approvedAmount", loan.getApprovedAmount());
                additionalData.put("repaymentPeriod", loan.getApprovedRepaymentPeriod());
                additionalData.put("monthlyPayment", loan.getApprovedMonthlyPayment());
                additionalData.put("remainingBalance", loan.getRemainingBalance());
                sendLoanEmailWithTemplate(loan.getEmployee().getEmail(),
                        "Loan Request Approved - Good News!",
                        "loan-approved",
                        loan,
                        additionalData);
            } catch (Exception e) {
                System.err.println("Failed to send loan approval to employee: " + e.getMessage());
            }
        }
        SystemSettings settings = settingsService.getSystemSettings();
        List<String> recipients = emailRecipientHelper.parseEmailRecipients(settings.getLoanApprovalRecipients());
        if (!recipients.isEmpty()) {
            Map<String, Object> additionalData = new HashMap<>();
            additionalData.put("feedback", feedback);
            additionalData.put("action", "approved");
            for (String recipient : recipients) {
                try {
                    sendLoanEmailWithTemplate(recipient,
                            "Loan Approved - " + getEmployeeName(loan.getEmployee()),
                            "loan-approval-notification",
                            loan,
                            additionalData);
                } catch (Exception e) {
                    System.err.println("Failed to send loan approval notification to " + recipient + ": " + e.getMessage());
                }
            }
        }
    }

    @Async
    @Override
    public void sendLoanRejectionNotification(LoanRequest loan, String feedback) {
        if (loan.getEmployee() != null && loan.getEmployee().getEmail() != null) {
            try {
                Map<String, Object> additionalData = new HashMap<>();
                additionalData.put("feedback", feedback);
                sendLoanEmailWithTemplate(loan.getEmployee().getEmail(),
                        "Loan Request Update - Status: Rejected",
                        "loan-rejected",
                        loan,
                        additionalData);
            } catch (Exception e) {
                System.err.println("Failed to send loan rejection to employee: " + e.getMessage());
            }
        }
        SystemSettings settings = settingsService.getSystemSettings();
        List<String> recipients = emailRecipientHelper.parseEmailRecipients(settings.getLoanRejectionRecipients());
        if (recipients == null || recipients.isEmpty()) {
            recipients = emailRecipientHelper.parseEmailRecipients(settings.getLoanApprovalRecipients());
        }
        if (!recipients.isEmpty()) {
            Map<String, Object> additionalData = new HashMap<>();
            additionalData.put("feedback", feedback);
            additionalData.put("action", "rejected");
            for (String recipient : recipients) {
                try {
                    sendLoanEmailWithTemplate(recipient,
                            "Loan Rejected - " + getEmployeeName(loan.getEmployee()),
                            "loan-rejection-notification",
                            loan,
                            additionalData);
                } catch (Exception e) {
                    System.err.println("Failed to send loan rejection notification to " + recipient + ": " + e.getMessage());
                }
            }
        }
    }

    @Async
    @Override
    public void sendLoanConsolidationNotification(LoanRequest consolidatedLoan, List<LoanRequest> originalLoans) {
        if (consolidatedLoan.getEmployee() != null && consolidatedLoan.getEmployee().getEmail() != null) {
            try {
                Map<String, Object> additionalData = new HashMap<>();
                additionalData.put("originalLoanCount", originalLoans.size());
                additionalData.put("consolidatedTotal", consolidatedLoan.getAggregatedTotal());
                additionalData.put("originalLoans", originalLoans);
                sendLoanEmailWithTemplate(consolidatedLoan.getEmployee().getEmail(),
                        "Loan Consolidation Complete - Important Information",
                        "loan-consolidated",
                        consolidatedLoan,
                        additionalData);
            } catch (Exception e) {
                System.err.println("Failed to send loan consolidation notification to employee: " + e.getMessage());
            }
        }
        SystemSettings settings = settingsService.getSystemSettings();
        List<String> recipients = emailRecipientHelper.parseEmailRecipients(settings.getLoanApprovalRecipients());
        if (!recipients.isEmpty()) {
            Map<String, Object> additionalData = new HashMap<>();
            additionalData.put("originalLoanCount", originalLoans.size());
            additionalData.put("consolidatedTotal", consolidatedLoan.getAggregatedTotal());
            additionalData.put("action", "consolidated");
            for (String recipient : recipients) {
                try {
                    sendLoanEmailWithTemplate(recipient,
                            "Loan Consolidation - " + getEmployeeName(consolidatedLoan.getEmployee()),
                            "loan-consolidation-notification",
                            consolidatedLoan,
                            additionalData);
                } catch (Exception e) {
                    System.err.println("Failed to send loan consolidation notification to " + recipient + ": " + e.getMessage());
                }
            }
        }
    }

    @Async
    @Override
    public void sendLeaveRequestToSupervisor(
            Leave leave,
            String supervisorEmail,
            String supervisorName
    ) {
        if (leave == null) {
            throw new IllegalArgumentException("Leave request is required");
        }

        if (supervisorEmail == null || supervisorEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Supervisor email is required");
        }

        Employee employee = leave.getEmployee();

        String employeeName = getEmployeeName(employee).trim();

        if (employeeName.isEmpty()) {
            employeeName = "Employee";
        }

        Map<String, Object> additionalData = new HashMap<>();

        additionalData.put(
                "supervisorName",
                supervisorName == null || supervisorName.isBlank()
                        ? "Supervisor"
                        : supervisorName.trim()
        );

        additionalData.put("employeeName", employeeName);
        additionalData.put("employee", employee);
        additionalData.put("leaveId", leave.getId());
        additionalData.put("leaveType", leave.getLeaveType());
        additionalData.put("startDate", leave.getStartDate());
        additionalData.put("endDate", leave.getEndDate());
        additionalData.put("reason", leave.getReason());
        additionalData.put("approvalToken", leave.getApprovalToken());

        additionalData.put(
                "reviewUrl",
                frontendUrlService.getBaseUrl() + "/#" +
                        "/leave?leaveId=" +
                        leave.getId() +
                        "&employeeId=" +
                        (employee != null ? employee.getId() : "")
        );

        sendHtmlEmailWithTemplateForApprover(
                supervisorEmail.trim(),
                "ACTION REQUIRED: Leave Request from " + employeeName,
                "leave-request-approver-notification",
                leave,
                employee,
                additionalData
        );

        System.out.println(
                "Leave request approval email sent to supervisor: " +
                        supervisorEmail
        );
    }

    // ==================== UTILITY METHODS ====================

    private String getEmployeeName(Employee employee) {
        if (employee == null) return "Employee";
        return (employee.getFirstName() != null ? employee.getFirstName() : "") + " " +
                (employee.getLastName() != null ? employee.getLastName() : "");
    }


    // ==================== PROCUREMENT PURCHASE NOTIFICATIONS ====================

    @Override
    public void sendProcurementPurchaseCreated(PurchaseOrder purchaseOrder) {
        if (purchaseOrder == null) return;

        SystemSettings settings = settingsService.getSystemSettings();
        if (Boolean.FALSE.equals(settings.getReviewerNotificationEnabled())) {
            return;
        }

        List<String> recipients =
                emailRecipientHelper.parseEmailRecipients(settings.getReviewerRecipients());

        if (recipients == null || recipients.isEmpty()) {
            System.out.println("No Procurement Purchase reviewer recipients configured.");
            return;
        }

        String subject = "New Procurement Purchase for Review - "
                + safe(purchaseOrder.getPurchaseOrderNumber());

        String html = buildProcurementPurchaseEmail(
                purchaseOrder,
                "A new Procurement Purchase has been created and is ready for review.",
                null,
                "REVIEW"
        );

        for (String recipient : recipients) {
            try {
                sendCustomEmail(recipient, subject, html);
                System.out.println("Procurement Purchase reviewer email sent to: " + recipient);
            } catch (Exception exception) {
                System.err.println(
                        "Failed to send Procurement Purchase reviewer email to "
                                + recipient + ": " + exception.getMessage()
                );
            }
        }
    }

    @Override
    public void sendProcurementPurchaseStoreNotification(
            PurchaseOrder purchaseOrder,
            String notificationNotes
    ) {
        if (purchaseOrder == null) return;

        SystemSettings settings = settingsService.getSystemSettings();
        if (Boolean.FALSE.equals(settings.getInventoryStoreNotificationEnabled())) {
            return;
        }

        List<String> recipients =
                emailRecipientHelper.parseEmailRecipients(settings.getInventoryStoreRecipients());

        if (recipients == null || recipients.isEmpty()) {
            System.out.println("No Procurement Purchase Store recipients configured.");
            return;
        }

        String subject = "Store Action Required - Procurement Purchase "
                + safe(purchaseOrder.getPurchaseOrderNumber());

        String html = buildProcurementPurchaseEmail(
                purchaseOrder,
                "Procurement has notified Store that this purchase is ready for Store action.",
                notificationNotes,
                "STORE"
        );

        for (String recipient : recipients) {
            try {
                sendCustomEmail(recipient, subject, html);
                System.out.println("Procurement Purchase Store email sent to: " + recipient);
            } catch (Exception exception) {
                System.err.println(
                        "Failed to send Procurement Purchase Store email to "
                                + recipient + ": " + exception.getMessage()
                );
            }
        }
    }

    private String buildProcurementPurchaseEmail(
            PurchaseOrder purchaseOrder,
            String headline,
            String notificationNotes,
            String audience
    ) {
        SystemSettings settings = settingsService.getSystemSettings();
        String companyName = safe(settings.getCompanyName());
        String purchaseNumber = safe(purchaseOrder.getPurchaseOrderNumber());
        String supplier = safe(purchaseOrder.getSupplierName());
        String orderedBy = safe(purchaseOrder.getOrderedBy());
        String status = safe(purchaseOrder.getStatus());

        StringBuilder itemsHtml = new StringBuilder();
        if (purchaseOrder.getItems() != null && !purchaseOrder.getItems().isEmpty()) {
            for (PurchaseOrderItem item : purchaseOrder.getItems()) {
                itemsHtml.append("<tr>")
                        .append("<td style=\"padding:9px;border-bottom:1px solid #e5e7eb;\">")
                        .append(escapeHtml(item.getProductName()))
                        .append("</td>")
                        .append("<td style=\"padding:9px;border-bottom:1px solid #e5e7eb;text-align:center;\">")
                        .append(item.getQuantity())
                        .append("</td>")
                        .append("<td style=\"padding:9px;border-bottom:1px solid #e5e7eb;text-align:right;\">GH₵ ")
                        .append(String.format("%,.2f", item.getUnitPrice()))
                        .append("</td>")
                        .append("<td style=\"padding:9px;border-bottom:1px solid #e5e7eb;text-align:right;\">GH₵ ")
                        .append(String.format("%,.2f", item.getTotalPrice()))
                        .append("</td>")
                        .append("</tr>");
            }
        } else {
            itemsHtml.append(
                    "<tr><td colspan=\"4\" style=\"padding:12px;text-align:center;color:#6b7280;\">No items</td></tr>"
            );
        }

        String actionText = "STORE".equals(audience)
                ? "Open the Store / Products page to review and receive the items."
                : "Open Procurement Purchases to review the purchase details.";

        String actionUrl = "STORE".equals(audience)
                ? frontendUrlService.route("/products")
                : frontendUrlService.route("/procurement-purchases");

        return "<!DOCTYPE html><html><body style=\"margin:0;background:#f4f6f8;font-family:Arial,sans-serif;color:#1f2937;\">"
                + "<div style=\"max-width:760px;margin:24px auto;background:#fff;border:1px solid #e5e7eb;border-radius:12px;overflow:hidden;\">"
                + "<div style=\"padding:24px 28px;background:#1d4ed8;color:#fff;\">"
                + "<h2 style=\"margin:0;font-size:22px;\">"
                + escapeHtml(companyName.isBlank() ? "Procurement Purchase" : companyName)
                + "</h2>"
                + "<p style=\"margin:8px 0 0;opacity:.9;\">Procurement Purchase Notification</p>"
                + "</div>"
                + "<div style=\"padding:26px 28px;\">"
                + "<p style=\"font-size:16px;margin-top:0;\">" + escapeHtml(headline) + "</p>"
                + "<table style=\"width:100%;border-collapse:collapse;margin:18px 0;background:#f9fafb;border-radius:8px;\">"
                + "<tr><td style=\"padding:9px;font-weight:600;\">Purchase No.</td><td style=\"padding:9px;\">"
                + escapeHtml(purchaseNumber) + "</td></tr>"
                + "<tr><td style=\"padding:9px;font-weight:600;\">Supplier</td><td style=\"padding:9px;\">"
                + escapeHtml(supplier) + "</td></tr>"
                + "<tr><td style=\"padding:9px;font-weight:600;\">Created By</td><td style=\"padding:9px;\">"
                + escapeHtml(orderedBy) + "</td></tr>"
                + "<tr><td style=\"padding:9px;font-weight:600;\">Status</td><td style=\"padding:9px;\">"
                + escapeHtml(status) + "</td></tr>"
                + "<tr><td style=\"padding:9px;font-weight:600;\">Total</td><td style=\"padding:9px;font-weight:700;\">GH₵ "
                + String.format("%,.2f", purchaseOrder.getTotalAmount())
                + "</td></tr>"
                + "</table>"
                + "<h3 style=\"font-size:16px;margin:22px 0 8px;\">Items</h3>"
                + "<table style=\"width:100%;border-collapse:collapse;border:1px solid #e5e7eb;\">"
                + "<thead><tr style=\"background:#f3f4f6;\">"
                + "<th style=\"padding:9px;text-align:left;\">Item</th>"
                + "<th style=\"padding:9px;text-align:center;\">Qty</th>"
                + "<th style=\"padding:9px;text-align:right;\">Unit Price</th>"
                + "<th style=\"padding:9px;text-align:right;\">Total</th>"
                + "</tr></thead><tbody>"
                + itemsHtml
                + "</tbody></table>"
                + (notificationNotes == null || notificationNotes.isBlank()
                ? ""
                : "<div style=\"margin-top:18px;padding:12px;background:#fff7ed;border-left:4px solid #f97316;\">"
                + "<strong>Procurement note:</strong><br/>"
                + escapeHtml(notificationNotes)
                + "</div>")
                + "<p style=\"margin-top:22px;\">" + escapeHtml(actionText) + "</p>"
                + "<p style=\"margin:20px 0 0;\">"
                + "<a href=\"" + actionUrl + "\" style=\"display:inline-block;background:#1d4ed8;color:#fff;text-decoration:none;padding:11px 18px;border-radius:7px;font-weight:600;\">Open System</a>"
                + "</p>"
                + "</div>"
                + "<div style=\"padding:16px 28px;background:#f9fafb;color:#6b7280;font-size:12px;\">"
                + "This is an automated Procurement Purchase notification."
                + "</div></div></body></html>";
    }

    // ==================== INVENTORY / PPE NOTIFICATIONS ====================

    @Async
    @Override
    public void sendPpeRequestCreated(InventoryRequest request, String requesterEmail) {
        SystemSettings settings = settingsService.getSystemSettings();
        String subject = "New PPE Request - " + safe(request == null ? null : request.getRequestNumber());
        String body = buildInventoryRequestEmail(
                request,
                "A new PPE request has been submitted.",
                requesterEmail,
                request == null ? null : request.getNotes()
        );

        Set<String> recipients = new LinkedHashSet<>();
        addRecipients(recipients, settings.getPpeRequestRecipients());
        addValidEmail(recipients, requesterEmail);
        addRequesterFromRequest(recipients, request);

        sendHtmlToRecipients(recipients, subject, body, "PPE request created");
    }

    @Async
    @Override
    public void sendInventoryRequestCreated(InventoryRequest request, String requesterEmail) {
        SystemSettings settings = settingsService.getSystemSettings();
        String subject = "New Inventory Request - " + safe(request == null ? null : request.getRequestNumber());
        String body = buildInventoryRequestEmail(
                request,
                "A new normal inventory request has been submitted.",
                requesterEmail,
                request == null ? null : request.getNotes()
        );

        Set<String> recipients = new LinkedHashSet<>();
        addRecipients(recipients, settings.getInventoryRequestRecipients());
        addValidEmail(recipients, requesterEmail);
        addRequesterFromRequest(recipients, request);

        sendHtmlToRecipients(recipients, subject, body, "inventory request created");
    }

    @Async
    @Override
    public void sendPpeStatusChanged(InventoryRequest request,
                                     String action,
                                     String performedBy,
                                     String notes) {
        SystemSettings settings = settingsService.getSystemSettings();
        String readableAction = readableAction(action);
        String subject = "PPE Request " + readableAction + " - "
                + safe(request == null ? null : request.getRequestNumber());
        String body = buildInventoryRequestEmail(request, readableAction, performedBy, notes);

        Set<String> recipients = new LinkedHashSet<>();
        addRecipientsForPpeStatus(recipients, settings, request == null ? null : request.getStatus());
        addRequesterFromRequest(recipients, request);

        sendHtmlToRecipients(recipients, subject, body, "PPE status changed");
    }

    @Async
    @Override
    public void sendInventoryStatusChanged(InventoryRequest request,
                                           String action,
                                           String performedBy,
                                           String notes) {
        SystemSettings settings = settingsService.getSystemSettings();
        String readableAction = readableAction(action);
        String subject = "Inventory Request " + readableAction + " - "
                + safe(request == null ? null : request.getRequestNumber());
        String body = buildInventoryRequestEmail(request, readableAction, performedBy, notes);

        Set<String> recipients = new LinkedHashSet<>();
        addRecipientsForNormalInventoryStatus(
                recipients,
                settings,
                request == null ? null : request.getStatus()
        );
        addRequesterFromRequest(recipients, request);

        sendHtmlToRecipients(recipients, subject, body, "inventory status changed");
    }

    private void addRecipientsForPpeStatus(Set<String> recipients,
                                           SystemSettings settings,
                                           RequestStatus status) {
        if (status == null) {
            addRecipients(recipients, settings.getPpeStatusRecipients());
            return;
        }

        switch (status) {
            case PENDING, PENDING_PLANNER ->
                    addRecipients(recipients, settings.getPpeRequestRecipients());

            case PROCUREMENT_PENDING, APPROVED_BY_PLANNER ->
                    addRecipients(recipients, settings.getPpeProcurementRecipients());

            case PARTIALLY_ISSUED, PROCURED, STORE_REVIEW, PENDING_STORE,
                 RECEIVED_IN_STORE, APPROVED_BY_STORE,
                 APPROVED_BY_PROCUREMENT ->
                    addRecipients(recipients, settings.getPpeStoreRecipients());

            case ISSUED, RECEIVED,
                 REJECTED_BY_PLANNER,
                 REJECTED_BY_PROCUREMENT,
                 REJECTED_BY_STORE,
                 CANCELLED ->
                    addRecipients(recipients, settings.getPpeStatusRecipients());
        }
    }

    private void addRecipientsForNormalInventoryStatus(Set<String> recipients,
                                                       SystemSettings settings,
                                                       RequestStatus status) {
        if (status == null) {
            addRecipients(recipients, settings.getInventoryStatusRecipients());
            return;
        }

        switch (status) {
            case PENDING, PENDING_PLANNER, APPROVED_BY_PLANNER ->
                    addRecipients(recipients, settings.getInventoryPlannerRecipients());

            case PROCUREMENT_PENDING, APPROVED_BY_PROCUREMENT ->
                    addRecipients(recipients, settings.getInventoryProcurementRecipients());

            case PENDING_STORE, STORE_REVIEW, PROCURED,
                 RECEIVED_IN_STORE, APPROVED_BY_STORE ->
                    addRecipients(recipients, settings.getInventoryStoreRecipients());

            case ISSUED, RECEIVED,
                 REJECTED_BY_PLANNER,
                 REJECTED_BY_PROCUREMENT,
                 REJECTED_BY_STORE,
                 CANCELLED ->
                    addRecipients(recipients, settings.getInventoryStatusRecipients());
        }
    }

    private void addRequesterFromRequest(Set<String> recipients, InventoryRequest request) {
        if (request == null) return;
        addValidEmail(recipients, request.getRequestedBy());
    }

    private void addRecipients(Set<String> recipients, String configuredEmails) {
        if (configuredEmails == null || configuredEmails.isBlank()) return;

        List<String> parsed = emailRecipientHelper.parseEmailRecipients(configuredEmails);
        if (parsed == null) return;

        parsed.forEach(email -> addValidEmail(recipients, email));
    }

    private void addValidEmail(Set<String> recipients, String email) {
        if (email == null) return;
        String cleaned = email.trim();
        if (cleaned.isEmpty() || !cleaned.contains("@")) return;
        recipients.add(cleaned.toLowerCase());
    }

    private void sendHtmlToRecipients(Set<String> recipients,
                                      String subject,
                                      String htmlBody,
                                      String eventName) {
        if (recipients == null || recipients.isEmpty()) {
            System.out.println("No configured recipients for " + eventName);
            return;
        }

        for (String recipient : recipients) {
            try {
                sendCustomEmail(recipient, subject, htmlBody);
                System.out.println("Inventory/PPE email sent to: " + recipient);
            } catch (Exception exception) {
                System.err.println(
                        "Failed to send " + eventName + " email to " + recipient
                                + ": " + exception.getMessage()
                );
            }
        }
    }

    private String buildInventoryRequestEmail(InventoryRequest request,
                                              String action,
                                              String performedBy,
                                              String notes) {
        SystemSettings settings = settingsService.getSystemSettings();
        String companyName = safe(settings.getCompanyName());
        boolean ppe = request != null && Boolean.TRUE.equals(request.getPpeRequest());
        String requestType = ppe ? "PPE Request" : "Inventory Request";

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><body style=\"margin:0;background:#f4f6f8;font-family:Arial,sans-serif;color:#1f2937;\">");
        html.append("<div style=\"max-width:760px;margin:24px auto;background:#ffffff;border:1px solid #e5e7eb;border-radius:12px;overflow:hidden;\">");
        html.append("<div style=\"padding:22px 26px;background:")
                .append(ppe ? "#047857" : "#1d4ed8")
                .append(";color:#ffffff;\">");
        html.append("<h2 style=\"margin:0;\">").append(escapeHtml(requestType)).append(" Notification</h2>");
        if (!companyName.isBlank()) {
            html.append("<p style=\"margin:6px 0 0;opacity:.9;\">").append(escapeHtml(companyName)).append("</p>");
        }
        html.append("</div><div style=\"padding:26px;\">");
        html.append("<p style=\"font-size:16px;\">").append(escapeHtml(action)).append("</p>");
        html.append("<table style=\"width:100%;border-collapse:collapse;margin-top:18px;\">");

        appendInventoryRow(html, "Request number", request == null ? null : request.getRequestNumber());
        appendInventoryRow(html, "Request type", requestType);
        appendInventoryRow(html, "Status", request == null ? null : request.getStatus());
        appendInventoryRow(html, "Requested by", request == null ? null : request.getRequestedBy());
        appendInventoryRow(html, "Department", request == null ? null : request.getDepartment());
        appendInventoryRow(html, "Project", request == null ? null : request.getProjectName());
        appendInventoryRow(html, "Project", request == null ? null : request.getJobName());
        appendInventoryRow(html, "Location", request == null ? null : request.getLocation());
        appendInventoryRow(html, "Urgency", request == null ? null : request.getUrgency());
        appendInventoryRow(html, "Total quantity", request == null ? null : request.getQuantity());
        appendInventoryRow(html, "Estimated cost", request == null ? null : request.getEstimatedCost());
        appendInventoryRow(html, "Action by", performedBy);
        appendInventoryRow(html, "Notes", notes);
        html.append("</table>");

        if (request != null && request.getItems() != null && !request.getItems().isEmpty()) {
            html.append("<h3 style=\"margin-top:26px;\">Requested items</h3>");
            html.append("<table style=\"width:100%;border-collapse:collapse;\">");
            html.append("<thead><tr style=\"background:#f3f4f6;\">")
                    .append("<th style=\"padding:10px;border:1px solid #d1d5db;text-align:left;\">Item</th>")
                    .append("<th style=\"padding:10px;border:1px solid #d1d5db;\">Requested</th>")
                    .append("<th style=\"padding:10px;border:1px solid #d1d5db;\">Approved</th>")
                    .append("<th style=\"padding:10px;border:1px solid #d1d5db;\">Issued</th>")
                    .append("</tr></thead><tbody>");

            for (RequestItem item : request.getItems()) {
                html.append("<tr>")
                        .append("<td style=\"padding:10px;border:1px solid #d1d5db;\">")
                        .append(escapeHtml(safe(item.getProductName())))
                        .append("</td>")
                        .append("<td style=\"padding:10px;border:1px solid #d1d5db;text-align:center;\">")
                        .append(escapeHtml(safe(item.getRequestedQuantity())))
                        .append("</td>")
                        .append("<td style=\"padding:10px;border:1px solid #d1d5db;text-align:center;\">")
                        .append(escapeHtml(safe(item.getApprovedQuantity())))
                        .append("</td>")
                        .append("<td style=\"padding:10px;border:1px solid #d1d5db;text-align:center;\">")
                        .append(escapeHtml(safe(item.getIssuedQuantity())))
                        .append("</td></tr>");
            }
            html.append("</tbody></table>");
        }

        if (request != null) {
            String viewUrl = frontendUrlService.route(inventoryViewRoute(request));
            html.append("<div style=\"margin-top:28px;text-align:center;\">")
                    .append("<a href=\"")
                    .append(escapeHtml(viewUrl))
                    .append("\" style=\"display:inline-block;padding:12px 22px;background:")
                    .append(ppe ? "#047857" : "#1d4ed8")
                    .append(";color:#ffffff;text-decoration:none;border-radius:8px;font-weight:bold;\">")
                    .append("View Request</a></div>");
        }

        html.append("<p style=\"margin-top:28px;color:#6b7280;font-size:13px;\">This is an automated notification.</p>");
        html.append("</div></div></body></html>");
        return html.toString();
    }

    private String inventoryViewRoute(InventoryRequest request) {
        RequestStatus status = request.getStatus();
        String requestId = request.getId() == null ? "" : "&requestId=" + request.getId();

        if (status == null) return "/InventoryRequest";

        return switch (status) {
            case PENDING, PENDING_PLANNER ->
                    "/plannerProductsReview";
            case APPROVED_BY_PLANNER, PROCUREMENT_PENDING ->
                    "/procurementManagerReview";
            case PARTIALLY_ISSUED, PROCURED, STORE_REVIEW, PENDING_STORE,
                 RECEIVED_IN_STORE, APPROVED_BY_STORE ->
                    "/received?tab=requests" + requestId;
            case ISSUED, RECEIVED ->
                    "/outgoing";
            case REJECTED_BY_PROCUREMENT ->
                    "/procurementManagerReview";
            case REJECTED_BY_STORE ->
                    "/received?tab=requests" + requestId;
            case REJECTED_BY_PLANNER, CANCELLED ->
                    "/InventoryRequest";
            case APPROVED_BY_PROCUREMENT ->
                    "/received?tab=requests" + requestId;
        };
    }

    private void appendInventoryRow(StringBuilder html, String label, Object value) {
        String stringValue = safe(value);
        if (stringValue.isBlank()) return;

        html.append("<tr>")
                .append("<td style=\"width:32%;padding:9px;border:1px solid #e5e7eb;background:#f9fafb;font-weight:bold;\">")
                .append(escapeHtml(label))
                .append("</td>")
                .append("<td style=\"padding:9px;border:1px solid #e5e7eb;\">")
                .append(escapeHtml(stringValue))
                .append("</td></tr>");
    }

    private String readableAction(String action) {
        if (action == null || action.isBlank()) return "Status Updated";
        String normalized = action.trim().replace('_', ' ').toLowerCase();
        StringBuilder result = new StringBuilder();
        for (String part : normalized.split("\\s+")) {
            if (part.isBlank()) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return result.toString();
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
