package com.eacattendance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@NoArgsConstructor
@AllArgsConstructor
@Entity
@Data
@Table(name = "attendance")
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;  //

    private String shift;
    private String workType;
    @Column(name = "date")
    private LocalDate date;
    private String status;
    private String leaveType;
    private double minimumHour;
    @Column(name = "checkIn") // Ensure this matches the database column name
    private LocalTime checkIn;

    @Column(nullable = false)
    private boolean biometric = false;

    @Column(name = "checkOut") // Ensure this matches the database column name
    private LocalTime checkOut;
    private double overtime;
    @Version // Ensures optimistic locking
    private Integer version;

    private double totalMinimumHour;

    private double totalOvertimeHour;

    @Column(name = "timestamp") // Optional: only if the column is explicitly named
    private LocalDateTime timestamp;

    private String action;

    private String method;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public String getShift() {
        return shift;
    }

    public void setShift(String shift) {
        this.shift = shift;
    }

    public String getWorkType() {
        return workType;
    }

    public void setWorkType(String workType) {
        this.workType = workType;
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

    public String getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(String leaveType) {
        this.leaveType = leaveType;
    }

    public double getMinimumHour() {
        return minimumHour;
    }

    public void setMinimumHour(double minimumHour) {
        this.minimumHour = minimumHour;
    }

    public LocalTime getCheckIn() {
        return checkIn;
    }

    public void setCheckIn(LocalTime checkIn) {
        this.checkIn = checkIn;
    }

    public boolean isBiometric() {
        return biometric;
    }

    public void setBiometric(boolean biometric) {
        this.biometric = biometric;
    }

    public LocalTime getCheckOut() {
        return checkOut;
    }

    public void setCheckOut(LocalTime checkOut) {
        this.checkOut = checkOut;
    }

    public double getOvertime() {
        return overtime;
    }

    public void setOvertime(double overtime) {
        this.overtime = overtime;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public double getTotalMinimumHour() {
        return totalMinimumHour;
    }

    public void setTotalMinimumHour(double totalMinimumHour) {
        this.totalMinimumHour = totalMinimumHour;
    }

    public double getTotalOvertimeHour() {
        return totalOvertimeHour;
    }

    public void setTotalOvertimeHour(double totalOvertimeHour) {
        this.totalOvertimeHour = totalOvertimeHour;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }
}
