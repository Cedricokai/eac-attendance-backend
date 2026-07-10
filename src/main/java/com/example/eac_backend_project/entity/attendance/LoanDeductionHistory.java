package com.example.eac_backend_project.entity.attendance;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "loan_deduction_history")
public class LoanDeductionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_id", nullable = false)
    private Long loanId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "payroll_period_id", nullable = false)
    private Long payrollPeriodId;

    @Column(name = "payroll_period_name")
    private String payrollPeriodName;

    @Column(name = "deduction_amount", nullable = false)
    private Double deductionAmount;

    private Double loanDeductionAmount;
    private LocalDateTime processedDate;
    private String processedBy;
    private Long payrollRecordId;
    private String deductionSnapshot;  // JSON string
    private String loanSummarySnapshot; // JSON string


    @Column(name = "remaining_balance_before")
    private Double remainingBalanceBefore;

    @Column(name = "remaining_balance_after", nullable = false)
    private Double remainingBalanceAfter;

    @Column(name = "applied_date", nullable = false)
    private LocalDateTime appliedDate;

    @Column(name = "applied_by")
    private String appliedBy;

    @Column(name = "is_reversed", nullable = false)
    private Boolean isReversed = false;

    @Column(name = "reversed_date")
    private LocalDateTime reversedDate;

    @Column(name = "reversed_by")
    private String reversedBy;

    @Column(name = "reversal_reason")
    private String reversalReason;

    @PrePersist
    protected void onCreate() {
        appliedDate = LocalDateTime.now();
        if (isReversed == null) {
            isReversed = false;
        }
    }
}