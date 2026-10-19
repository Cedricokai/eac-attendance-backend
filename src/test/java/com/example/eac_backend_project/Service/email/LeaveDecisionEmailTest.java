package com.example.eac_backend_project.Service.email;

import com.example.eac_backend_project.Service.settings.SettingsService;
import com.example.eac_backend_project.Service.FrontendUrlService;
import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.attendance.Leave;
import com.example.eac_backend_project.entity.settings.SystemSettings;
import com.example.eac_backend_project.utils.EmailRecipientHelper;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class LeaveDecisionEmailTest {
    @org.junit.jupiter.api.AfterEach void clearAuthentication() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }
    @Test void allDecisionPathsNotifyEmployeeAndObserversOnce() throws Exception {
        for (int action = 0; action < 5; action++) {
            var mail = mock(JavaMailSender.class);
            var templates = mock(TemplateEngine.class);
            var settings = mock(SettingsService.class);
            var recipients = mock(EmailRecipientHelper.class);
            var urls = mock(FrontendUrlService.class);
            var config = new SystemSettings(); config.setLeaveApprovalRecipients("list");
            when(settings.getSystemSettings()).thenReturn(config);
            when(recipients.parseEmailRecipients("list")).thenReturn(List.of(" Employee@example.com ", "observer@example.com"));
            when(mail.createMimeMessage()).thenAnswer(i -> new MimeMessage((Session)null));
            var contexts = new ArrayList<Context>();
            when(templates.process(eq("leave-decision-notification"), any(Context.class))).thenAnswer(i -> {
                contexts.add(i.getArgument(1)); return "Decision";
            });
            var service = new EmailServiceImpl(mail, templates, settings, recipients, urls);
            var users = mock(com.example.eac_backend_project.repository.UserRepository.class);
            var approver = new com.example.eac_backend_project.entity.inventory.User();
            approver.setName("Alex Mensah");
            when(users.findByUserName("alex")).thenReturn(Optional.of(approver));
            ReflectionTestUtils.setField(service, "userRepository", users);
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                            "alex", null, List.of()));
            ReflectionTestUtils.setField(service,"smtpUsername","sender@example.com");
            var employee = new Employee(); employee.setEmail("employee@example.com"); employee.setFirstName("Jane");
            var leave = new Leave(); leave.setEmployee(employee);
            leave.setStatus(action < 2 ? "Supervisor_Approved" : action == 4 ? "Rejected" : "Approved");
            switch(action) {
                case 0 -> service.sendSupervisorApprovalNotification(leave,"feedback");
                case 1 -> service.sendPlannerApprovalNotification(leave,"feedback");
                case 2 -> service.sendHrApprovalNotification(leave,"feedback");
                case 3 -> service.sendBulkApprovalNotification(leave,"feedback");
                case 4 -> service.sendLeaveRejectionNotification(leave,"hr","feedback");
            }
            verify(mail,times(2)).send(any(MimeMessage.class));
            assertEquals(true,contexts.get(0).getVariable("employeeNotice"));
            assertEquals(false,contexts.get(1).getVariable("employeeNotice"));
            assertEquals(action == 2 || action == 3,contexts.get(0).getVariable("finalApproval"));
            assertEquals(action == 4,contexts.get(0).getVariable("rejected"));
            assertEquals("feedback",contexts.get(0).getVariable("feedback"));
            assertEquals("Alex Mensah", contexts.get(0).getVariable("decisionActor"));
            assertEquals("Alex Mensah", contexts.get(1).getVariable("decisionActor"));
        }
    }

    @Test void failedEmployeeDeliveryDoesNotBlockObserver() {
        var mail = mock(JavaMailSender.class);
        var settings = mock(SettingsService.class);
        var recipients = mock(EmailRecipientHelper.class);
        var config = new SystemSettings(); config.setLeaveApprovalRecipients("list");
        when(settings.getSystemSettings()).thenReturn(config);
        when(recipients.parseEmailRecipients("list")).thenReturn(List.of("observer@example.com"));
        when(mail.createMimeMessage()).thenThrow(new IllegalStateException("delivery failed"))
                .thenReturn(new MimeMessage((Session)null));
        var templates = mock(TemplateEngine.class);
        when(templates.process(anyString(),any(Context.class))).thenReturn("Decision");
        var service = new EmailServiceImpl(mail,templates,settings,recipients,mock(FrontendUrlService.class));
        ReflectionTestUtils.setField(service,"smtpUsername","sender@example.com");
        var employee = new Employee(); employee.setEmail("employee@example.com");
        var leave = new Leave(); leave.setEmployee(employee); leave.setStatus("Rejected");
        service.sendLeaveRejectionNotification(leave,"hr","reason");
        verify(mail).send(any(MimeMessage.class));
    }
}
