package com.eacattendance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "overview")
public class Overview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "attendance_id", nullable = true)
    private Attendance attendance;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;
    @Version // Ensures optimistic locking
    private Integer version;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "overtime_id", nullable = true)
    private Overtime overtime;
    @Column(name = "overtime_rate")
    private Double overtimeRate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "leave_id", nullable = true)
    private Leave leave;

    private String shift;
    private LocalDate date;
    private Double hoursWorked;
     private String status;
    // Change these to double instead of Attendance type
    private Double minimumHour;


    @Column(nullable = true)  // Explicitly mark as nullable
    private Double overtimeHours;

    public Double getHoursWorked() {
        return hoursWorked != null ? hoursWorked : 0.0; // Return 0.0 if null
    }

    public void setHoursWorked(Double hoursWorked) {
        this.hoursWorked = hoursWorked != null ? hoursWorked : 0.0; // Set to 0.0 if null
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Attendance getAttendance() {
        return attendance;
    }

    public void setAttendance(Attendance attendance) {
        this.attendance = attendance;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Overtime getOvertime() {
        return overtime;
    }

    public void setOvertime(Overtime overtime) {
        this.overtime = overtime;
    }

    public Double getOvertimeRate() {
        return overtimeRate;
    }

    public void setOvertimeRate(Double overtimeRate) {
        this.overtimeRate = overtimeRate;
    }

    public Leave getLeave() {
        return leave;
    }

    public void setLeave(Leave leave) {
        this.leave = leave;
    }

    public String getShift() {
        return shift;
    }

    public void setShift(String shift) {
        this.shift = shift;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getMinimumHour() {
        return minimumHour;
    }

    public void setMinimumHour(Double minimumHour) {
        this.minimumHour = minimumHour;
    }

    public Double getOvertimeHours() {
        return overtimeHours;
    }

    public void setOvertimeHours(Double overtimeHours) {
        this.overtimeHours = overtimeHours;
    }
}
