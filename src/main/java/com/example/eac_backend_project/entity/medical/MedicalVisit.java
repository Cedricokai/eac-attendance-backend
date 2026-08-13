package com.example.eac_backend_project.entity.medical;

import com.example.eac_backend_project.entity.attendance.Employee;
import jakarta.persistence.*;
import lombok.Data;
import java.time.*;

@Data
@Entity
@Table(name = "medical_visits", indexes = {
        @Index(name = "idx_medical_visit_status", columnList = "status"),
        @Index(name = "idx_medical_visit_employee", columnList = "employee_id")})
public class MedicalVisit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(unique = true, nullable = false, length = 40) private String referenceNumber;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Hospital hospital;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Employee employee;
    @Column(nullable = false) private LocalDate visitDate;
    private LocalTime arrivalTime;
    private LocalTime dischargeTime;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private VisitType visitType;
    @Column(nullable = false) private boolean requiresExcuseDuty;
    private LocalDate excuseStartDate;
    private LocalDate excuseEndDate;
    @Column(nullable = false) private boolean fitToReturn;
    private LocalDate followUpDate;
    private String hospitalReference;
    @Column(length = 2000) private String remarks;
    @Column(nullable = false) private String submittedByName;
    @Column(nullable = false) private String submittedByContact;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private VisitStatus status = VisitStatus.PENDING_VERIFICATION;
    @Column(length = 2000) private String hrComment;
    private Boolean paidExcuseDuty;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private String attachmentPath;
    private String attachmentName;
    private String attachmentContentType;
    private LocalDateTime createdAt;
    @PrePersist void created() { if (createdAt == null) createdAt = LocalDateTime.now(); }
    public enum VisitType { OUTPATIENT, EMERGENCY, ADMISSION, FOLLOW_UP }
    public enum VisitStatus { PENDING_VERIFICATION, VERIFIED, REJECTED }
}
