package com.example.eac_backend_project.entity.lunch;

import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.inventory.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "lunch_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LunchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private String action;

    @Column(columnDefinition = "TEXT")
    private String details;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by")
    private User performedBy;

    @Column(name = "lunch_date", nullable = false)
    private LocalDate lunchDate;

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    // Fixed: Removed nullable = false to allow existing NULL values
    @Column(name = "performed_date")
    private LocalDateTime performedDate;

    @PrePersist
    protected void onCreate() {
        // Set creation date
        this.createdDate = LocalDateTime.now();

        // Set default lunch date if not provided
        if (this.lunchDate == null) {
            this.lunchDate = LocalDate.now();
        }

        // Set performed date if not provided
        if (this.performedDate == null) {
            this.performedDate = LocalDateTime.now();
        }
    }
}