package com.example.eac_backend_project.entity.attendance;

import com.example.eac_backend_project.entity.inventory.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Getter
@Setter
@Table(name = "attendance_request", indexes = {
        @Index(name = "idx_attendance_request_status", columnList = "status"),
        @Index(name = "idx_attendance_request_employee_date", columnList = "employee_id,attendance_date")
})
public class AttendanceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate date;

    @Column(name = "check_in", nullable = false)
    private LocalTime checkIn;

    @Column(name = "check_out", nullable = false)
    private LocalTime checkOut;

    @Column(nullable = false, length = 40)
    private String shift;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Column(name = "evidence_reference", length = 255)
    private String evidenceReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceRequestStatus status = AttendanceRequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_user_id", nullable = false)
    private User requestedBy;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_id", unique = true)
    private Attendance attendance;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Integer version;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        requestedAt = requestedAt == null ? now : requestedAt;
        createdAt = now;
        updatedAt = now;
        status = AttendanceRequestStatus.PENDING;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
