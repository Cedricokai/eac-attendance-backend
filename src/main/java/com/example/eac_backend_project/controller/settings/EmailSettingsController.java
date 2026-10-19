package com.example.eac_backend_project.controller.settings;

import com.example.eac_backend_project.Service.settings.SettingsService;
import com.example.eac_backend_project.dto.EmailConfigurationDTO;
import com.example.eac_backend_project.dto.SystemSettingsDTO;
import com.example.eac_backend_project.entity.settings.SystemSettings;
import com.example.eac_backend_project.utils.EmailRecipientHelper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/settings/email")
public class EmailSettingsController {

    private static final Logger logger = LoggerFactory.getLogger(EmailSettingsController.class);

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private EmailRecipientHelper emailRecipientHelper;

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping("/recipients")
    public ResponseEntity<EmailConfigurationDTO> getEmailRecipients() {
        SystemSettings settings = settingsService.getSystemSettings();

        EmailConfigurationDTO dto = new EmailConfigurationDTO();
        dto.setLeaveRequestRecipients(emailRecipientHelper.parseEmailRecipients(settings.getLeaveRequestRecipients()));
        dto.setAttendanceAlertRecipients(emailRecipientHelper.parseEmailRecipients(settings.getAttendanceAlertRecipients()));
        dto.setLateArrivalRecipients(emailRecipientHelper.parseEmailRecipients(settings.getLateArrivalRecipients()));
        dto.setOvertimeRequestRecipients(emailRecipientHelper.parseEmailRecipients(settings.getOvertimeRequestRecipients()));
        dto.setAttendanceValidationRecipients(emailRecipientHelper.parseEmailRecipients(settings.getAttendanceValidationRecipients()));
        dto.setLeaveApprovalRecipients(emailRecipientHelper.parseEmailRecipients(settings.getLeaveApprovalRecipients()));
        dto.setAccountRequestRecipients(emailRecipientHelper.parseEmailRecipients(settings.getAccountRequestRecipients()));
        dto.setSupportRequestRecipients(emailRecipientHelper.parseEmailRecipients(settings.getSupportRequestRecipients()));
        dto.setPettyCashRequestRecipients(emailRecipientHelper.parseEmailRecipients(settings.getPettyCashRequestRecipients()));

        return ResponseEntity.ok(dto);
    }

    @PutMapping("/recipients")
    public ResponseEntity<EmailConfigurationDTO> updateEmailRecipients(@RequestBody EmailConfigurationDTO dto) {
        // Get current settings
        SystemSettings currentSettings = settingsService.getSystemSettings();

        // Update the email recipient fields
        currentSettings.setLeaveRequestRecipients(emailRecipientHelper.formatEmailList(dto.getLeaveRequestRecipients()));
        currentSettings.setAttendanceAlertRecipients(emailRecipientHelper.formatEmailList(dto.getAttendanceAlertRecipients()));
        currentSettings.setLateArrivalRecipients(emailRecipientHelper.formatEmailList(dto.getLateArrivalRecipients()));
        currentSettings.setOvertimeRequestRecipients(emailRecipientHelper.formatEmailList(dto.getOvertimeRequestRecipients()));
        currentSettings.setAttendanceValidationRecipients(emailRecipientHelper.formatEmailList(dto.getAttendanceValidationRecipients()));
        currentSettings.setLeaveApprovalRecipients(emailRecipientHelper.formatEmailList(dto.getLeaveApprovalRecipients()));
        currentSettings.setAccountRequestRecipients(emailRecipientHelper.formatEmailList(dto.getAccountRequestRecipients()));
        currentSettings.setSupportRequestRecipients(emailRecipientHelper.formatEmailList(dto.getSupportRequestRecipients()));
        currentSettings.setPettyCashRequestRecipients(emailRecipientHelper.formatEmailList(dto.getPettyCashRequestRecipients()));

        // Convert SystemSettings to SystemSettingsDTO for the service method
        SystemSettingsDTO settingsDTO = convertToDTO(currentSettings);

        // Update using the service method which expects DTO
        settingsService.updateSystemSettings(settingsDTO);

        return ResponseEntity.ok(dto);
    }

    // Helper method to convert SystemSettings entity to SystemSettingsDTO
    private SystemSettingsDTO convertToDTO(SystemSettings settings) {
        SystemSettingsDTO dto = new SystemSettingsDTO();

        // Basic settings
        dto.setHourlyRate(settings.getHourlyRate());
        dto.setOvertimeHourlyRate(settings.getOvertimeHourlyRate());
        dto.setStandardWorkHours(settings.getStandardWorkHours());
        dto.setDoubleTimeOnSunday(settings.getDoubleTimeOnSunday());
        dto.setTimeAndHalfAfter8Hours(settings.getTimeAndHalfAfter8Hours());
        dto.setWeekendRate(settings.getWeekendRate());
        dto.setHolidayRate(settings.getHolidayRate());
        dto.setLateArrivalTime(settings.getLateArrivalTime());

        // Company info
        dto.setCompanyName(settings.getCompanyName());
        dto.setCompanyEmail(settings.getCompanyEmail());
        dto.setCompanyPhone(settings.getCompanyPhone());
        dto.setCompanyAddress(settings.getCompanyAddress());

        // Overtime multipliers
        dto.setDefaultOvertimeMultiplier(settings.getDefaultOvertimeMultiplier());
        dto.setSundayOvertimeMultiplier(settings.getSundayOvertimeMultiplier());
        dto.setHolidayOvertimeMultiplier(settings.getHolidayOvertimeMultiplier());
        dto.setEnableTimeAndHalfAfter8Hours(settings.getEnableTimeAndHalfAfter8Hours());
        dto.setTimeAndHalfMultiplier(settings.getTimeAndHalfMultiplier());

        // Billing settings
        dto.setBillingReminderEmails(settings.getBillingReminderEmails());
        dto.setBillingReminderEnabled(settings.getBillingReminderEnabled());

        // Email recipient fields
        dto.setLeaveRequestRecipients(settings.getLeaveRequestRecipients());
        dto.setAttendanceAlertRecipients(settings.getAttendanceAlertRecipients());
        dto.setLateArrivalRecipients(settings.getLateArrivalRecipients());
        dto.setOvertimeRequestRecipients(settings.getOvertimeRequestRecipients());
        dto.setAttendanceValidationRecipients(settings.getAttendanceValidationRecipients());
        dto.setLeaveApprovalRecipients(settings.getLeaveApprovalRecipients());
        dto.setAccountRequestRecipients(settings.getAccountRequestRecipients());
        dto.setSupportRequestRecipients(settings.getSupportRequestRecipients());
        dto.setPettyCashRequestRecipients(settings.getPettyCashRequestRecipients());

        // Convert employeeCategories from JSON string to List<String>
        List<String> employeeCategories = new ArrayList<>();
        if (settings.getEmployeeCategories() != null && !settings.getEmployeeCategories().trim().isEmpty()) {
            try {
                employeeCategories = objectMapper.readValue(
                        settings.getEmployeeCategories(),
                        new TypeReference<List<String>>() {}
                );
            } catch (Exception e) {
                logger.error("Error parsing employee categories: {}", e.getMessage());
                employeeCategories = new ArrayList<>();
            }
        }
        dto.setEmployeeCategories(employeeCategories); // This should be List<String>, not String

        // Convert weekendDays from JSON string to List<Integer>
        List<Integer> weekendDays = new ArrayList<>();
        if (settings.getWeekendDays() != null && !settings.getWeekendDays().trim().isEmpty()) {
            try {
                weekendDays = objectMapper.readValue(
                        settings.getWeekendDays(),
                        new TypeReference<List<Integer>>() {}
                );
            } catch (Exception e) {
                logger.error("Error parsing weekend days: {}", e.getMessage());
                weekendDays = new ArrayList<>();
            }
        }
        dto.setWeekendDays(weekendDays); // This should be List<Integer>, not String

        return dto;
    }
}
