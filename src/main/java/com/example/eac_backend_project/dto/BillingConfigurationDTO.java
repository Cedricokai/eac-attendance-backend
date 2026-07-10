package com.example.eac_backend_project.dto;

import lombok.Data;
import java.time.LocalTime;

@Data
public class BillingConfigurationDTO {
    private String reminderDaysBefore; // e.g., "1,3,7"
    private LocalTime reminderTime;
    private LocalTime overdueReminderTime;
    private Boolean enableAdvanceReminders;
    private Boolean enableDueDateReminders;
    private Boolean enableOverdueReminders;
    private Integer maxRetryAttempts;
    private Integer retryDelayMinutes;
}