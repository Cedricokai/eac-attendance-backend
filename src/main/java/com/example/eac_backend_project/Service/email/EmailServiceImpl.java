package com.example.eac_backend_project.Service.email;

import com.example.eac_backend_project.Service.settings.SettingsService;
import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.attendance.Job;
import com.example.eac_backend_project.entity.attendance.Leave;
import com.example.eac_backend_project.entity.attendance.LoanRequest;
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

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final SettingsService settingsService;
    private final EmailRecipientHelper emailRecipientHelper;

    @Value("${spring.mail.username}")
    private String smtpUsername;

    @Value("${app.email.base-url:http://localhost:5173}")
    private String baseUrl;

    @Autowired
    public EmailServiceImpl(JavaMailSender mailSender, TemplateEngine templateEngine,
                            SettingsService settingsService, EmailRecipientHelper emailRecipientHelper) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.settingsService = settingsService;
        this.emailRecipientHelper = emailRecipientHelper;
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
            variables.put("baseUrl", baseUrl);
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
            variables.put("baseUrl", baseUrl);
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

    @Override
    public void sendSupervisorApprovalNotification(Leave leave, String feedback) {
        sendHtmlEmailWithTemplate(
                leave.getEmployee().getEmail(),
                "Leave Approved by Supervisor",
                "supervisor-approval-request",
                leave,
                leave.getEmployee()
        );
        SystemSettings settings = settingsService.getSystemSettings();
        List<String> recipients = emailRecipientHelper.parseEmailRecipients(settings.getLeaveApprovalRecipients());
        if (!recipients.isEmpty()) {
            for (String recipient : recipients) {
                try {
                    sendHtmlEmailWithTemplate(recipient,
                            "Leave Approved by Supervisor - " + leave.getEmployee().getFirstName(),
                            "supervisor-approval-request",
                            leave,
                            leave.getEmployee());
                } catch (Exception e) {
                    System.err.println("Failed to send email to " + recipient + ": " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void sendPlannerApprovalNotification(Leave leave, String feedback) {
        sendHtmlEmailWithTemplate(
                leave.getEmployee().getEmail(),
                "Leave Approved by Planner",
                "planner-approval-request",
                leave,
                leave.getEmployee()
        );
        SystemSettings settings = settingsService.getSystemSettings();
        List<String> recipients = emailRecipientHelper.parseEmailRecipients(settings.getLeaveApprovalRecipients());
        if (!recipients.isEmpty()) {
            for (String recipient : recipients) {
                try {
                    sendHtmlEmailWithTemplate(recipient,
                            "Leave Approved by Planner - " + leave.getEmployee().getFirstName(),
                            "planner-approval-request",
                            leave,
                            leave.getEmployee());
                } catch (Exception e) {
                    System.err.println("Failed to send email to " + recipient + ": " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void sendHrApprovalNotification(Leave leave, String feedback) {
        sendHtmlEmailWithTemplate(
                leave.getEmployee().getEmail(),
                "Leave Approved by HR",
                "hr-approval-request",
                leave,
                leave.getEmployee()
        );
        SystemSettings settings = settingsService.getSystemSettings();
        List<String> recipients = emailRecipientHelper.parseEmailRecipients(settings.getLeaveApprovalRecipients());
        if (!recipients.isEmpty()) {
            for (String recipient : recipients) {
                try {
                    sendHtmlEmailWithTemplate(recipient,
                            "Leave Approved by HR - " + leave.getEmployee().getFirstName(),
                            "hr-approval-request",
                            leave,
                            leave.getEmployee());
                } catch (Exception e) {
                    System.err.println("Failed to send email to " + recipient + ": " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void sendLeaveRejectionNotification(Leave leave, String role, String feedback) {
        sendHtmlEmailWithTemplate(
                leave.getEmployee().getEmail(),
                "Leave Request Rejected",
                "leave-rejection-notification",
                leave,
                leave.getEmployee()
        );
        SystemSettings settings = settingsService.getSystemSettings();
        List<String> recipients = emailRecipientHelper.parseEmailRecipients(settings.getLeaveApprovalRecipients());
        if (!recipients.isEmpty()) {
            for (String recipient : recipients) {
                try {
                    sendHtmlEmailWithTemplate(recipient,
                            "Leave Rejected - " + leave.getEmployee().getFirstName(),
                            "leave-rejection-notification",
                            leave,
                            leave.getEmployee());
                } catch (Exception e) {
                    System.err.println("Failed to send email to " + recipient + ": " + e.getMessage());
                }
            }
        }
    }

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
            } catch (Exception e) {
                System.err.println("Failed to send overtime notification to " + recipient + ": " + e.getMessage());
            }
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
            } catch (Exception e) {
                System.err.println("Failed to send validation notification to " + recipient + ": " + e.getMessage());
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
        } catch (Exception ex) {
            throw new RuntimeException("Failed to send email with Excel attachment: " + ex.getMessage(), ex);
        }
    }

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
            variables.put("baseUrl", baseUrl);
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
            variables.put("baseUrl", baseUrl);
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
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send billing enlisted email: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendPasswordResetEmail(String to, String resetLink, String userName) {
        try {
            SystemSettings systemSettings = settingsService.getSystemSettings();
            String companyName = systemSettings.getCompanyName();
            String companyEmail = systemSettings.getCompanyEmail();
            String companyPhone = systemSettings.getCompanyPhone();
            String companyAddress = systemSettings.getCompanyAddress();
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());
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
            Context context = new Context();
            context.setVariables(variables);
            String html = templateEngine.process("password-reset", context);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to send password reset email: " + ex.getMessage(), ex);
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

    private void sendLoanEmailWithTemplate(String to, String subject, String templateName,
                                           LoanRequest loan, Map<String, Object> additionalData) {
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
            variables.put("baseUrl", baseUrl);
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

    private String getEmployeeName(Employee employee) {
        if (employee == null) return "Employee";
        return (employee.getFirstName() != null ? employee.getFirstName() : "") + " " +
                (employee.getLastName() != null ? employee.getLastName() : "");
    }
}