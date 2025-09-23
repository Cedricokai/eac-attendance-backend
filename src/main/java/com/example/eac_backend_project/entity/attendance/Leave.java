package com.example.eac_backend_project.entity.attendance;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Data
@Table(name = "leave_requests")
public class Leave {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne
    @JoinColumn(name = "employee_id")
    private Employee employee;
    @ManyToOne
    @JoinColumn(name = "attendance_id")
    private Attendance attendance;
    @ManyToOne
    @JoinColumn(name = "overtime_id")
    private Overtime overtime;

    private LocalDate startDate;
    private LocalDate endDate;
    private String leaveType;
    private String reason;

    private String status;


    // Approval tracking
    private String supervisorStatus;
    private String supervisorFeedback;
    private LocalDateTime supervisorActionDate;

    private String plannerStatus;
    private String plannerFeedback;
    private LocalDateTime plannerActionDate;

    private String hrStatus;
    private String hrFeedback;
    private LocalDateTime hrActionDate;

    private String attachment;
    private LocalDateTime submittedDate;

    @PrePersist
    protected void onCreate() {
        submittedDate = LocalDateTime.now();
        status = "Pending";
        supervisorStatus = "Pending";
        plannerStatus = "Pending";
        hrStatus = "Pending";
    }

    public String getSupervisorStatus() {
        return supervisorStatus;
    }

    public void setSupervisorStatus(String supervisorStatus) {
        this.supervisorStatus = supervisorStatus;
    }

    public String getSupervisorFeedback() {
        return supervisorFeedback;
    }

    public void setSupervisorFeedback(String supervisorFeedback) {
        this.supervisorFeedback = supervisorFeedback;
    }

    public LocalDateTime getSupervisorActionDate() {
        return supervisorActionDate;
    }

    public void setSupervisorActionDate(LocalDateTime supervisorActionDate) {
        this.supervisorActionDate = supervisorActionDate;
    }

    public String getPlannerStatus() {
        return plannerStatus;
    }

    public void setPlannerStatus(String plannerStatus) {
        this.plannerStatus = plannerStatus;
    }

    public String getPlannerFeedback() {
        return plannerFeedback;
    }

    public void setPlannerFeedback(String plannerFeedback) {
        this.plannerFeedback = plannerFeedback;
    }

    public LocalDateTime getPlannerActionDate() {
        return plannerActionDate;
    }

    public void setPlannerActionDate(LocalDateTime plannerActionDate) {
        this.plannerActionDate = plannerActionDate;
    }

    public String getHrStatus() {
        return hrStatus;
    }

    public void setHrStatus(String hrStatus) {
        this.hrStatus = hrStatus;
    }

    public String getHrFeedback() {
        return hrFeedback;
    }

    public void setHrFeedback(String hrFeedback) {
        this.hrFeedback = hrFeedback;
    }

    public LocalDateTime getHrActionDate() {
        return hrActionDate;
    }

    public void setHrActionDate(LocalDateTime hrActionDate) {
        this.hrActionDate = hrActionDate;
    }

    public String getAttachment() {
        return attachment;
    }

    public void setAttachment(String attachment) {
        this.attachment = attachment;
    }

    public LocalDateTime getSubmittedDate() {
        return submittedDate;
    }

    public void setSubmittedDate(LocalDateTime submittedDate) {
        this.submittedDate = submittedDate;
    }

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

    public Attendance getAttendance() {
        return attendance;
    }

    public void setAttendance(Attendance attendance) {
        this.attendance = attendance;
    }

    public Overtime getOvertime() {
        return overtime;
    }

    public void setOvertime(Overtime overtime) {
        this.overtime = overtime;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(String leaveType) {
        this.leaveType = leaveType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}










