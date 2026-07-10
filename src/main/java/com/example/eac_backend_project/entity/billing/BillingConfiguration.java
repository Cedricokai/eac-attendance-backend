package com.example.eac_backend_project.entity.billing;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "billing_configuration")
@Data
public class BillingConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reminder_days_before", nullable = false)
    private String reminderDaysBefore = "3,7"; // Comma-separated days (e.g., "1,3,7")

    @Column(name = "reminder_time", nullable = false)
    private LocalTime reminderTime = LocalTime.of(8, 0); // Default 8:00 AM

    @Column(name = "overdue_reminder_time")
    private LocalTime overdueReminderTime = LocalTime.of(10, 0); // Default 10:00 AM

    @Column(name = "enable_advance_reminders", nullable = false)
    private Boolean enableAdvanceReminders = true;

    @Column(name = "enable_due_date_reminders", nullable = false)
    private Boolean enableDueDateReminders = true;

    @Column(name = "enable_overdue_reminders", nullable = false)
    private Boolean enableOverdueReminders = true;

    @Column(name = "max_retry_attempts")
    private Integer maxRetryAttempts = 3;

    @Column(name = "retry_delay_minutes")
    private Integer retryDelayMinutes = 30;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public BillingConfiguration() {}
}