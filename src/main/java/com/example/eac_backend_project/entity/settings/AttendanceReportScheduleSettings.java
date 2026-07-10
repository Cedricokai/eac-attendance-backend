package com.example.eac_backend_project.entity.settings;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "attendance_report_schedule_settings")
@Data
public class AttendanceReportScheduleSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="enabled", nullable=false)
    private boolean enabled = false;

    @Column(name="send_time", nullable=false)
    private LocalTime sendTime = LocalTime.of(18, 0);

    @Column(name = "last_sent_date")
    private LocalDate lastSentDate;


    @Column(name="recipient_emails", columnDefinition = "TEXT")
    private String recipientEmails;

    @Column(name="send_only_exceptions", nullable=false)
    private boolean sendOnlyExceptions = true;

    @Column(name="attach_excel", nullable=false)
    private boolean attachExcel = true;

    @Column(name = "auto_absent_enabled", nullable = false)
    private boolean autoAbsentEnabled = true;  // Enable/disable auto-absent marking

    @Column(name = "auto_absent_run_time", nullable = false)
    private LocalTime autoAbsentRunTime = LocalTime.of(0, 30);  // Default 00:30 AM

    @ElementCollection
    @CollectionTable(name = "auto_absent_skip_categories",
            joinColumns = @JoinColumn(name = "schedule_settings_id"))
    @Column(name = "category_id")
    private List<Long> autoAbsentSkipCategories = new ArrayList<>();

    public Long getId() { return id; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public LocalTime getSendTime() { return sendTime; }
    public void setSendTime(LocalTime sendTime) { this.sendTime = sendTime; }

    public String getRecipientEmails() { return recipientEmails; }
    public void setRecipientEmails(String recipientEmails) { this.recipientEmails = recipientEmails; }

    public boolean isSendOnlyExceptions() { return sendOnlyExceptions; }
    public void setSendOnlyExceptions(boolean sendOnlyExceptions) { this.sendOnlyExceptions = sendOnlyExceptions; }

    public boolean isAttachExcel() { return attachExcel; }
    public void setAttachExcel(boolean attachExcel) { this.attachExcel = attachExcel; }
}
