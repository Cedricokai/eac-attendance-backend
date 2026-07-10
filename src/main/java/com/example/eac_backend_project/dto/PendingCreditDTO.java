package com.example.eac_backend_project.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingCreditDTO {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private Double amount;
    private String reason;
    private LocalDateTime createdDate;
    private String createdBy;
    private String status;
    private String appliedToPeriodName;  // ← Add this
    private Long appliedToPeriodId;       // ← Add this
    private LocalDateTime appliedDate;
}