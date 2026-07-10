package com.example.eac_backend_project.entity.attendance;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "pending_credits")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingCredit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private Double amount;

    private String reason;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "created_by")
    private String createdBy;

    @Column(nullable = false)
    private String status; // PENDING, APPLIED, CANCELLED

    @ManyToOne
    @JoinColumn(name = "applied_to_period_id")
    private PayrollPeriod appliedToPeriod;

    @Column(name = "applied_date")
    private LocalDateTime appliedDate;
}