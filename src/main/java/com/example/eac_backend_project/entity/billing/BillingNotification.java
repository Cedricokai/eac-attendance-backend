package com.example.eac_backend_project.entity.billing;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(
        name = "billing_notifications",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_billing_notice",
                        columnNames = {"job_id", "recipient_email", "due_date", "notice_type", "scheduled_date"}
                )
        }
)
@Data
public class BillingNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "notice_type", nullable = false)
    private String noticeType; // ADVANCE_7_DAYS, ADVANCE_3_DAYS, ADVANCE_1_DAY, DUE_TODAY, OVERDUE

    @Column(name = "days_before_due")
    private Integer daysBeforeDue; // For advance notices: 7, 3, 1

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate; // When this notification should be sent

    @Column(name = "scheduled_time")
    private LocalTime scheduledTime; // When this notification should be sent

    @Column(name = "status", nullable = false)
    private String status; // PENDING, SENT, FAILED, CANCELLED

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    public BillingNotification() {}

    public BillingNotification(Long jobId, String recipientEmail, LocalDate dueDate,
                               String noticeType, Integer daysBeforeDue,
                               LocalDate scheduledDate, LocalTime scheduledTime) {
        this.jobId = jobId;
        this.recipientEmail = recipientEmail;
        this.dueDate = dueDate;
        this.noticeType = noticeType;
        this.daysBeforeDue = daysBeforeDue;
        this.scheduledDate = scheduledDate;
        this.scheduledTime = scheduledTime;
        this.status = "PENDING";
        this.retryCount = 0;
    }

    public Long getId() { return id; }

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }

    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public String getNoticeType() { return noticeType; }
    public void setNoticeType(String noticeType) { this.noticeType = noticeType; }

    public Integer getDaysBeforeDue() { return daysBeforeDue; }
    public void setDaysBeforeDue(Integer daysBeforeDue) { this.daysBeforeDue = daysBeforeDue; }

    public LocalDate getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(LocalDate scheduledDate) { this.scheduledDate = scheduledDate; }

    public LocalTime getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(LocalTime scheduledTime) { this.scheduledTime = scheduledTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
}