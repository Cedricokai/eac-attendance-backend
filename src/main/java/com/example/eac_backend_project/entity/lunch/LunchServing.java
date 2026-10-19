package com.example.eac_backend_project.entity.lunch;

import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.inventory.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "lunch_serving")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LunchServing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "lunch_date", nullable = false)
    private LocalDate lunchDate;

    @Column(name = "lunch_time")
    private LocalTime lunchTime;

    @Column(name = "serving_time")
    private LocalDateTime servingTime = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "served_by", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User servedBy;

    private String device;
    private String remarks;

    @Column(name = "attendance_status")
    private String attendanceStatus;

    @Column(name = "location")
    private String location;

    // ✅ ADD THIS FIELD
    @Column(name = "server_name")
    private String serverName;

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        if (servingTime == null) {
            servingTime = LocalDateTime.now();
        }
        if (lunchTime == null && servingTime != null) {
            lunchTime = servingTime.toLocalTime();
        }
        // ✅ Set default server name if not provided
        if (serverName == null || serverName.isEmpty()) {
            serverName = "default-server";
        }
    }
}
