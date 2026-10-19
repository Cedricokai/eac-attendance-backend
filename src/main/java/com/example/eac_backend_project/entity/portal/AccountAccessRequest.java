package com.example.eac_backend_project.entity.portal;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "account_access_requests", indexes = {@Index(name = "idx_account_request_status", columnList = "status"), @Index(name = "idx_account_request_employee", columnList = "employee_id")})
@Data
public class AccountAccessRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 32) private String reference;
    @Column(name = "full_name", nullable = false, length = 150) private String fullName;
    @Column(name = "employee_id", nullable = false, length = 80) private String employeeId;
    @Column(nullable = false, length = 120) private String department;
    @Column(name = "work_email", nullable = false, length = 180) private String workEmail;
    @Column(nullable = false, length = 40) private String mobile;
    @Column(name = "access_reason", nullable = false, columnDefinition = "TEXT") private String accessReason;
    @Column(nullable = false, length = 24) private String status = "PENDING";
    @Column(name = "review_notes", columnDefinition = "TEXT") private String reviewNotes;
    @Column(name = "reviewed_by", length = 120) private String reviewedBy;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "reviewed_at") private LocalDateTime reviewedAt;
}
