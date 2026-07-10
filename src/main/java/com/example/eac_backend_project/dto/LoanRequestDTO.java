package com.example.eac_backend_project.dto;

import lombok.Data;

@Data
public class LoanRequestDTO {
    private Long employeeId;
    private double amount;
    private String purpose;
    private int repaymentPeriod;
    private double monthlyPaymentAmount;
    private Boolean consolidateWithExisting;
}
