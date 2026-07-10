package com.example.eac_backend_project.entity;

import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.attendance.Leave;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_notifications")
@Data
public class EmailNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Column(nullable = false)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "notification_type", nullable = false)
    private String notificationType; // e.g., "LEAVE_REQUEST", "APPROVAL", "REJECTION"

    @Column(nullable = false)
    private String status; // e.g., "PENDING", "SENT", "FAILED"

    @Column(name = "is_read", nullable = false) // CHANGED FROM 'read' to 'is_read'
    private Boolean read = false;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_id", nullable = false)
    private Leave leave;


}