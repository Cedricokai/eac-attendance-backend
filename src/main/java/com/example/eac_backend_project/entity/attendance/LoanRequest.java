package com.example.eac_backend_project.entity.attendance;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "loan_requests")
public class LoanRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "employee_id", nullable = false)
    @JsonIgnoreProperties({"loans", "attendances", "leaves"})
    private Employee employee;

    // Original requested amounts
    private double loanAmount;
    private double monthlyPaymentAmount;
    private String purpose;
    private int repaymentPeriod;

    // Approved/Modified amounts (null if not approved or no modification)
    private Double approvedAmount;
    private Double approvedMonthlyPayment;
    private Integer approvedRepaymentPeriod;

    private Double paidAmount = 0.0;

    // New fields for loan aggregation
    @Column(name = "remaining_balance")
    private Double remainingBalance;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "is_aggregated")
    private Boolean isAggregated = false;

    @Column(name = "previous_loan_id")
    private Long previousLoanId;

    @Column(name = "aggregated_from_loan_ids", columnDefinition = "LONGTEXT")
    private String aggregatedFromLoanIds;
    // JSON array of original loan IDs

    @Column(name = "original_loan_amount")
    private Double originalLoanAmount; // The amount before aggregation

    @Column(name = "aggregated_total")
    private Double aggregatedTotal; // Total including existing loans

    @Column(name = "consolidation_date")
    private LocalDateTime consolidationDate;

    @Column(name = "consolidated_by")
    private String consolidatedBy;

    // Additional fields for active/inactive tracking
    @Column(name = "deactivated_by")
    private String deactivatedBy;

    @Column(name = "deactivation_reason")
    private String deactivationReason;

    @Column(name = "deactivation_date")
    private LocalDateTime deactivationDate;

    @Column(name = "reactivated_by")
    private String reactivatedBy;

    @Column(name = "reactivation_date")
    private LocalDateTime reactivationDate;

    @Column(name = "completed_date")
    private LocalDateTime completedDate;

    @Column(name = "last_deduction_period_id")
    private Long lastDeductionPeriodId;

    @Column(name = "last_deduction_date")
    private LocalDateTime lastDeductionDate;

// Getters and setters for all new fields

    // Tracking fields
    private String status; // Pending, Approved, Rejected, Aggregated
    private String processedBy;
    private String processingNotes;
    private LocalDateTime processedDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime requestDate;

    @PrePersist
    protected void onCreate() {
        this.requestDate = LocalDateTime.now();
        if (this.status == null || this.status.isEmpty()) {
            this.status = "Pending";
        }
        if (this.isActive == null) {
            this.isActive = true;
        }
        if (this.isAggregated == null) {
            this.isAggregated = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if ("Approved".equals(this.status) || "Rejected".equals(this.status) || "Aggregated".equals(this.status)) {
            this.processedDate = LocalDateTime.now();
        }
    }

    // Helper method to calculate remaining balance
    public double calculateRemainingBalance() {
        double total = this.approvedAmount != null ? this.approvedAmount : this.loanAmount;
        double paid = this.paidAmount != null ? this.paidAmount : 0.0;
        return total - paid;
    }
}