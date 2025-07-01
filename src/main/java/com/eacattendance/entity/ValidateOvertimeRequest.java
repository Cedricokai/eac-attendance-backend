package com.eacattendance.entity;


import java.util.List;

public class ValidateOvertimeRequest {
    private List<Long> overtimeIds;

    // Getters and setters
    public List<Long> getOvertimeIds() {
        return overtimeIds;
    }

    public void setOvertimeIds(List<Long> overtimeIds) {
        this.overtimeIds = overtimeIds;
    }
}