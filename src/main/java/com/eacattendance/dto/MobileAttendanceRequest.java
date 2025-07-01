package com.eacattendance.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class MobileAttendanceRequest {
    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    @NotBlank(message = "Timestamp is required")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$",
            message = "Timestamp must be in ISO format (yyyy-MM-ddTHH:mm:ss)")
    private String timestamp;

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "(?i)CHECK_IN|CHECK_OUT",
            message = "Status must be either CHECK_IN or CHECK_OUT")
    private String status;
}