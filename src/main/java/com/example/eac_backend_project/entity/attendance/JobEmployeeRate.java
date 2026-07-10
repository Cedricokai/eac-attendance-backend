package com.example.eac_backend_project.entity.attendance;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_employee_rate", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"job_id", "employee_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobEmployeeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "normal_hour_rate", nullable = false)
    private Double normalHourRate;  // NH rate per hour

    @Column(name = "overtime_rate", nullable = false)
    private Double overtimeRate;    // OT rate per hour

    @Column(name = "effective_from")
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}