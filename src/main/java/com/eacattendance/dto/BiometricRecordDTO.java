package com.eacattendance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.AssertTrue;
import lombok.Data;

import java.time.DateTimeException;
import java.time.LocalDateTime;

@Data
public class BiometricRecordDTO {
    private String action; // "SIGN ON" or "SIGN OFF"
    private String authenticationMethod;
    private String employeeName;
    private Long employeeId;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getAuthenticationMethod() {
        return authenticationMethod;
    }

    public void setAuthenticationMethod(String authenticationMethod) {
        this.authenticationMethod = authenticationMethod;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @JsonFormat(pattern = "yyyy/MM/dd-HH:mm:ss")
    private LocalDateTime timestamp;

    // Add validation
    @AssertTrue(message = "Timestamp must be valid")
    public boolean isTimestampValid() {
        try {
            return timestamp != null && timestamp.getYear() > 1900;
        } catch (DateTimeException e) {
            return false;
        }
    }
}
