package com.example.eac_backend_project.entity.attendance;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Data
@Table(name = "overtime")
public class Overtime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_id", nullable = true)
    private Attendance attendance;

    private String notes;

    @Column(name = "category", nullable = true)
    private String category;

    // Add this field to your Overtime entity
    @Column(nullable = true)
    private Boolean multiplierAppliedToHours = false;  // true = applied to hours, false = applied to rate

    private LocalDate date;
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime startTime;
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime endTime;
    private Double overtimeHours;
    private String status = "Pending";

    private Double rawOvertimeHours;

    // New fields for multiplier effect
    @Column(nullable = true)

    private Double baseHourlyRate;
    @Column(nullable = true)
    private Double overtimeMultiplier; // <-- no default here

    private Double calculatedOvertimePay;

    // Audit fields
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
        updatedAt = java.time.LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public Attendance getAttendance() { return attendance; }
    public void setAttendance(Attendance attendance) { this.attendance = attendance; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public Double getOvertimeHours() { return overtimeHours; }
    public void setOvertimeHours(Double overtimeHours) { this.overtimeHours = overtimeHours; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Double getBaseHourlyRate() { return baseHourlyRate; }
    public void setBaseHourlyRate(Double baseHourlyRate) { this.baseHourlyRate = baseHourlyRate; }
    public Double getOvertimeMultiplier() { return overtimeMultiplier; }
    public void setOvertimeMultiplier(Double overtimeMultiplier) { this.overtimeMultiplier = overtimeMultiplier; }
    public Double getCalculatedOvertimePay() { return calculatedOvertimePay; }
    public void setCalculatedOvertimePay(Double calculatedOvertimePay) { this.calculatedOvertimePay = calculatedOvertimePay; }
    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }
    public java.time.LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(java.time.LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}