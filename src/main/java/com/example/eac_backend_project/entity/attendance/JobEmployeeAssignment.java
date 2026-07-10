package com.example.eac_backend_project.entity.attendance;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_employee_assignment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobEmployeeAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "temp_nh_rate")
    private Double tempNhRate;          // billing NH rate (per hour)

    @Column(name = "temp_ot_rate")
    private Double tempOtRate;          // billing OT rate (per hour)

    @Column(name = "is_supervisor")
    private Boolean isSupervisor = false;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}