package com.example.eac_backend_project.Service.email;

import com.example.eac_backend_project.Exceptions.ResourceNotFoundException;
import com.example.eac_backend_project.Service.settings.SettingsService;
import com.example.eac_backend_project.config.EmailTemplatesConfig;
import com.example.eac_backend_project.entity.EmailNotification;
import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.attendance.Leave;
import com.example.eac_backend_project.repository.EmployeeRepository;
import com.example.eac_backend_project.repository.UserRepository;
import com.example.eac_backend_project.repository.email.EmailNotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class EmailNotificationService {

    @Autowired
    private EmailNotificationRepository emailNotificationRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private EmailTemplatesConfig templatesConfig;

    @Autowired
    private SettingsService settingsService;

    // In EmailNotificationService.java - Update sendLeaveRequestNotifications
    @Async
    public void sendLeaveRequestNotifications(Leave leave) {
        Employee employee = leave.getEmployee();
        if (employee == null) return;

        // Send to all configured recipients from settings
        emailService.sendLeaveRequestNotificationToAll(leave);

        // Get category-based approvers
        List<String> supervisorEmails = getSupervisorEmailsForEmployee(employee);

        // Send to category-specific supervisors
        for (String supervisorEmail : supervisorEmails) {
            if (supervisorEmail != null && !supervisorEmail.isEmpty() &&
                    !supervisorEmail.equals(employee.getEmail())) {

                String supervisorTemplate = templatesConfig.getLeave().getSupervisorApprovalRequest();
                createAndSendLeaveNotification(
                        leave,
                        "SUPERVISOR_APPROVAL_REQUEST",
                        "Leave Request Pending Approval - " + employee.getEmployeeId(),
                        supervisorTemplate,
                        supervisorEmail
                );
            }
        }
    }

    // Helper method to get supervisor emails based on employee's category
    private List<String> getSupervisorEmailsForEmployee(Employee employee) {
        List<String> emails = new ArrayList<>();

        if (employee.getCategory() != null) {
            SettingsService.CategoryApprovers approvers = settingsService.getCategoryApprovers(employee.getCategory().getId());

            if (approvers.isUseCustomApprovers() && approvers.getSupervisorEmails() != null) {
                // Use category-specific supervisor emails
                emails.addAll(approvers.getSupervisorEmails());
            } else {
                // Fallback to global settings or employee's direct supervisor
                if (employee.getSupervisor() != null && employee.getSupervisor().getEmail() != null) {
                    emails.add(employee.getSupervisor().getEmail());
                }
            }
        }

        // If still no emails, try to find any supervisor in the system
        if (emails.isEmpty()) {
            userRepository.findByRoleNameAndEnabled("ROLE_SUPERVISOR")
                    .forEach(user -> employeeRepository.findByEmail(user.getEmail())
                            .ifPresent(sup -> emails.add(sup.getEmail())));
        }

        return emails;
    }

    // Add similar methods for planner and HR emails
    public List<String> getPlannerEmailsForEmployee(Employee employee) {
        List<String> emails = new ArrayList<>();

        if (employee.getCategory() != null) {
            SettingsService.CategoryApprovers approvers = settingsService.getCategoryApprovers(employee.getCategory().getId());

            if (approvers.isUseCustomApprovers() && approvers.getPlannerEmails() != null) {
                emails.addAll(approvers.getPlannerEmails());
            }
        }

        // Fallback to global
        if (emails.isEmpty()) {
            userRepository.findByRoleNameAndEnabled("ROLE_PLANNER")
                    .forEach(user -> employeeRepository.findByEmail(user.getEmail())
                            .ifPresent(planner -> emails.add(planner.getEmail())));
        }

        return emails;
    }

    public List<String> getHrEmailsForEmployee(Employee employee) {
        List<String> emails = new ArrayList<>();

        if (employee.getCategory() != null) {
            SettingsService.CategoryApprovers approvers = settingsService.getCategoryApprovers(employee.getCategory().getId());

            if (approvers.isUseCustomApprovers() && approvers.getHrEmails() != null) {
                emails.addAll(approvers.getHrEmails());
            }
        }

        // Fallback to global
        if (emails.isEmpty()) {
            userRepository.findByRoleNameAndEnabled("ROLE_HR")
                    .forEach(user -> employeeRepository.findByEmail(user.getEmail())
                            .ifPresent(hr -> emails.add(hr.getEmail())));
        }

        return emails;
    }
    private List<Employee> findSupervisorsForEmployee(Employee employee) {
        List<Employee> supervisors = new ArrayList<>();
        if (employee.getSupervisor() != null) supervisors.add(employee.getSupervisor());
        if (employee.getJob() != null && employee.getJob().getSupervisor() != null) {
            Employee jobSupervisor = employee.getJob().getSupervisor();
            if (!supervisors.contains(jobSupervisor)) supervisors.add(jobSupervisor);
        }
        if (employee.getJob() != null) {
            List<Employee> jobSupervisors = employeeRepository.findSupervisorsByJobId(employee.getJob().getId());
            for (Employee js : jobSupervisors) if (!supervisors.contains(js)) supervisors.add(js);
        }
        if (supervisors.isEmpty()) {
            userRepository.findByRoleNameAndEnabled("ROLE_SUPERVISOR")
                    .forEach(user -> employeeRepository.findByEmail(user.getEmail()).ifPresent(supervisors::add));
        }
        return supervisors;
    }

    @Async
    public void createAndSendLeaveNotification(Leave leave, String type, String subject, String templateName, String recipientEmail) {
        EmailNotification notification = new EmailNotification();
        notification.setRecipientEmail(recipientEmail);
        notification.setSubject(subject);
        notification.setNotificationType(type);
        notification.setLeave(leave);
        notification.setEmployee(leave.getEmployee());
        notification.setStatus("PENDING");
        emailNotificationRepository.save(notification);

        try {
            emailService.sendHtmlEmailWithTemplate(recipientEmail, subject, templateName, leave, notification.getEmployee());
            notification.setStatus("SENT");
            notification.setSentAt(LocalDateTime.now());
        } catch (Exception e) {
            notification.setStatus("FAILED");
            notification.setErrorMessage(e.getMessage());
        }
        emailNotificationRepository.save(notification);
    }

    public List<EmailNotification> getNotificationsByEmployee(Long employeeId) {
        return emailNotificationRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId);
    }

    public long getUnreadCount(String email) {
        return emailNotificationRepository.countByRecipientEmailAndReadFalse(email);
    }

    @Transactional
    public void markAllAsRead(String email) {
        List<EmailNotification> unread = emailNotificationRepository.findByReadFalseAndRecipientEmail(email);
        unread.forEach(n -> n.setRead(true));
        emailNotificationRepository.saveAll(unread);
    }

    // In EmailNotificationService.java
    @Transactional
    public void deleteByLeaveId(Long leaveId) {
        emailNotificationRepository.deleteByLeaveId(leaveId);
    }

    // Get all notifications for a user (by email)
    public List<EmailNotification> getNotificationsForUser(String email) {
        return emailNotificationRepository.findByRecipientEmailOrderByCreatedAtDesc(email);
    }

    // Get unread count
    public long getUnreadCountForUser(String email) {
        return emailNotificationRepository.countByRecipientEmailAndReadFalse(email);
    }

    // Mark a single notification as read
    @Transactional
    public void markAsRead(Long notificationId, String userEmail) {
        EmailNotification notif = emailNotificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        // Security check: ensure the notification belongs to the user
        if (!notif.getRecipientEmail().equals(userEmail)) {
            throw new SecurityException("You are not authorized to access this notification");
        }
        notif.setRead(true);
        emailNotificationRepository.save(notif);
    }


}
