package com.example.eac_backend_project.entity.attendance;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "raw_biometric_event", indexes = {
        @Index(name = "idx_raw_bio_event_time", columnList = "event_timestamp"),
        @Index(name = "idx_raw_bio_received", columnList = "received_at"),
        @Index(name = "idx_raw_bio_employee", columnList = "employee_id"),
        @Index(name = "idx_raw_bio_source_status", columnList = "source_type,processing_status")
})
@Getter @Setter
public class RawBiometricEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "source_type", nullable = false, length = 30)
    private String sourceType;
    @Column(name = "device_id", length = 100)
    private String deviceId;
    @Column(name = "device_event_id", length = 150)
    private String deviceEventId;
    @Column(name = "device_user_id", length = 100)
    private String deviceUserId;
    @Column(name = "raw_employee_name", length = 255)
    private String rawEmployeeName;
    @Column(name = "event_timestamp")
    private LocalDateTime eventTimestamp;
    @Column(name = "punch_type", length = 50)
    private String punchType;
    @Column(name = "verification_method", length = 80)
    private String verificationMethod;
    @Lob @Column(name = "raw_payload", nullable = false, columnDefinition = "LONGTEXT")
    private String rawPayload;
    @Column(name = "received_at", nullable = false, updatable = false)
    private LocalDateTime receivedAt;
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    @Column(name = "processing_status", nullable = false, length = 30)
    private String processingStatus;
    @Column(name = "batch_id", length = 100)
    private String batchId;
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "employee_id")
    private Employee employee;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "attendance_id")
    private Attendance attendance;

    @PrePersist void prePersist() {
        if (receivedAt == null) receivedAt = LocalDateTime.now();
        if (processingStatus == null) processingStatus = "RECEIVED";
    }
}