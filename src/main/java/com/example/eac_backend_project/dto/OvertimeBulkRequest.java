package com.example.eac_backend_project.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class OvertimeBulkRequest {
    private List<Long> employeeIds;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;
    private Boolean multiplierAppliedToHours;
    private String notes;

    // Constructors
    public OvertimeBulkRequest() {}

    public OvertimeBulkRequest(List<Long> employeeIds, LocalDate date, LocalTime startTime,
                               LocalTime endTime, String status, Boolean multiplierAppliedToHours, String notes) {
        this.employeeIds = employeeIds;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.multiplierAppliedToHours = multiplierAppliedToHours;
        this.notes = notes;
    }

    // Getters and Setters
    public List<Long> getEmployeeIds() {
        return employeeIds;
    }

    public void setEmployeeIds(List<Long> employeeIds) {
        this.employeeIds = employeeIds;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getMultiplierAppliedToHours() {
        return multiplierAppliedToHours;
    }

    public void setMultiplierAppliedToHours(Boolean multiplierAppliedToHours) {
        this.multiplierAppliedToHours = multiplierAppliedToHours;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}