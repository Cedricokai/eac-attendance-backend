package com.example.eac_backend_project.dto;

import lombok.Data;

@Data
public class LoanStatusUpdateDTO {
    private String status; // "Approved" or "Rejected"
    private Double approvedAmount;
    private Integer approvedRepaymentPeriod;
    private Double approvedMonthlyPayment;
    private String notes;
    private String processedBy; // Username or ID of the approver
    private Boolean consolidateWithExisting; // New field
    private String consolidationReason;
}