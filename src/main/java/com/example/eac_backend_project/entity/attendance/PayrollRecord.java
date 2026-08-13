package com.example.eac_backend_project.entity.attendance;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
public class PayrollRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @ManyToOne
    @JoinColumn(name = "period_id")
    private PayrollPeriod period;

    private Double tier2Amount;
    private Double creditAmount = 0.0;
    private Double regularPay;
    private Double overtimePay;
    private int workingDays;
    private Double totalHours;
    private String status; // "Generated", "Processed"
    private Double overtimeHours;

    @Column(name = "loan_deduction")
    private Double loanDeduction = 0.0; // Amount deducted this period

    @Column(name = "loan_details", columnDefinition = "TEXT")
    private String loanDetails; // JSON of active loans

    @Column(name = "has_active_loans")
    private Boolean hasActiveLoans = false;

    @Column(name = "remaining_loan_balance")
    private Double remainingLoanBalance = 0.0; // Balance after this deduction

    @Column(columnDefinition = "TEXT")
    private String loanSummary; // JSON with aggregated loan info

    // Add these fields to your PayrollRecord entity
    @Column(name = "loan_deduction_total")
    private Double loanDeductionTotal = 0.0;  // Total monthly loan repayment

    @Column(name = "active_loans_count")
    private Integer activeLoansCount = 0;  // Number of active loans

    @Column(name = "loan_details_json", columnDefinition = "TEXT")
    private String loanDetailsJson;  // Store loan details as JSON for display

    @Column(name = "has_loans")
    private Boolean hasLoans = false;  // Quick flag if employee has loans

    @Column(name = "weighted_overtime_hours")
    private Double weightedOvertimeHours = 0.0;

    private Double tax;

    @Column(name = "applied_credit_ids", length = 500)
    private String appliedCreditIds;

    private Double nonTaxableIncome;

    @Column(name = "include_basic_salary")
    private Boolean includeBasicSalary = true;


    @Column(name = "use_non_taxable_allowances")
    private Boolean useNonTaxableAllowances = false;

    // Add to existing PayrollRecord entity class

    @Column(name = "pre_loan_net_salary")
    private Double preLoanNetSalary;

    @Column(name = "pre_loan_loan_deduction")
    private Double preLoanLoanDeduction;

    @Column(name = "loan_details_snapshot", columnDefinition = "TEXT")
    private String loanDetailsSnapshot;

    @Column(name = "reversal_reason")
    private String reversalReason;

    @Column(name = "reversed_at")
    private LocalDateTime reversedAt;

    @Column(name = "reversed_by")
    private String reversedBy;

    // In PayrollRecord.java - Add this field if not present
    @Column(name = "loan_deduction_processed")
    private Boolean loanDeductionProcessed = false;

// Getters and Setters for all new fields

    // Ghana-specific fields
    private Double basicSalary;
    private Double incomeTax;
    private Double grossPay;
    private Double netPay;
    private Double ssnitEmployeeContribution;
    private Double ssnitEmployerContribution;
    private Double totalSsnitContribution;
    private Double taxableIncome;
    private Double otherDeductions;

    private Double grossSalary;
    private Double ssnitEmployee;
    private Double ssnitEmployer;
    private Double payeTax;
    private Double netSalary;
    private Integer totalWorkingDays;

    @Column(name = "holiday_days")
    private Integer holidayDays;

    @Column(name = "weekend_days")
    private Integer weekendDays;

    @Column(name = "holiday_hours")
    private Double holidayHours;

    @Column(name = "weekend_hours")
    private Double weekendHours;

    @Column(name = "multiplier_bonus")
    private Double multiplierBonus;

    // Add new field for leave hours
    @Column(name = "leave_hours")
    private Double leaveHours = 0.0;

    @Column(name = "leave_days")
    private Integer leaveDays = 0;

    @Column(name = "total_days_in_period")
    private Integer totalDaysInPeriod = 0;



    @Column(name = "total_leave_hours")
    private Double totalLeaveHours = 0.0;

    @Column(name = "uses_temporary_allowances")
    private Boolean usesTemporaryAllowances = false;

    public Boolean getUsesTemporaryAllowances() {
        return usesTemporaryAllowances;
    }

    public void setUsesTemporaryAllowances(Boolean usesTemporaryAllowances) {
        this.usesTemporaryAllowances = usesTemporaryAllowances;
    }

    public Boolean getUseNonTaxableAllowances() {
        return useNonTaxableAllowances;
    }

    public void setUseNonTaxableAllowances(Boolean useNonTaxableAllowances) {
        this.useNonTaxableAllowances = useNonTaxableAllowances;
    }

    public Integer getHolidayDays() {
        return holidayDays;
    }

    public void setHolidayDays(Integer holidayDays) {
        this.holidayDays = holidayDays;
    }

    public Integer getWeekendDays() {
        return weekendDays;
    }

    public void setWeekendDays(Integer weekendDays) {
        this.weekendDays = weekendDays;
    }

    public Double getHolidayHours() {
        return holidayHours;
    }

    public void setHolidayHours(Double holidayHours) {
        this.holidayHours = holidayHours;
    }

    public Double getWeekendHours() {
        return weekendHours;
    }

    public void setWeekendHours(Double weekendHours) {
        this.weekendHours = weekendHours;
    }

    public Double getMultiplierBonus() {
        return multiplierBonus;
    }

    public void setMultiplierBonus(Double multiplierBonus) {
        this.multiplierBonus = multiplierBonus;
    }

    public Double getTotalLeaveHours() {
        return totalLeaveHours;
    }

    public void setTotalLeaveHours(Double totalLeaveHours) {
        this.totalLeaveHours = totalLeaveHours;
    }

    public Double getLeaveHours() {
        return leaveHours;
    }

    public void setLeaveHours(Double leaveHours) {
        this.leaveHours = leaveHours;
    }

    public Integer getTotalWorkingDays() {
        return totalWorkingDays;
    }

    public void setTotalWorkingDays(Integer totalWorkingDays) {
        this.totalWorkingDays = totalWorkingDays;
    }

    public Integer getLeaveDays() {
        return leaveDays;
    }

    public void setLeaveDays(Integer leaveDays) {
        this.leaveDays = leaveDays;
    }

    public Integer getTotalDaysInPeriod() {
        return totalDaysInPeriod;
    }

    public void setTotalDaysInPeriod(Integer totalDaysInPeriod) {
        this.totalDaysInPeriod = totalDaysInPeriod;
    }

    // ===== Allowances (split + totals) =====
    private Double rentAllowance = 0.0;
    private Double transportAllowance = 0.0;  // TnT
    private Double clothingAllowance = 0.0;
    private Double otherAllowance = 0.0;
    private Double nssAllowance = 0.0;

    private Double totalAllowances = 0.0;
    private Double taxableAllowances = 0.0;
    private Double nonTaxableAllowances = 0.0;

    // ================== Helpers ==================
    private Double safe(Double value) {
        return value != null ? value : 0.0;
    }

    private void recalcAllowances() {
        this.totalAllowances = safe(rentAllowance) + safe(transportAllowance) +
                safe(clothingAllowance) + safe(otherAllowance) + safe(nssAllowance);
        this.taxableAllowances = this.totalAllowances;
        this.nonTaxableAllowances = 0.0;
    }

    // Override setters to auto-update
    public void setRentAllowance(Double rentAllowance) {
        this.rentAllowance = safe(rentAllowance);
        recalcAllowances();
    }

    public void setTransportAllowance(Double transportAllowance) {
        this.transportAllowance = safe(transportAllowance);
        recalcAllowances();
    }

    public void setClothingAllowance(Double clothingAllowance) {
        this.clothingAllowance = safe(clothingAllowance);
        recalcAllowances();
    }

    public void setOtherAllowance(Double otherAllowance) {
        this.otherAllowance = safe(otherAllowance);
        recalcAllowances();
    }

    public void setNssAllowance(Double nssAllowance) {
        this.nssAllowance = safe(nssAllowance);
        recalcAllowances();
    }

    // Safe getters
    public Double getTotalAllowances() { return safe(totalAllowances); }
    public Double getTaxableAllowances() { return safe(taxableAllowances); }
    public Double getNonTaxableAllowances() { return safe(nonTaxableAllowances); }

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

    public PayrollPeriod getPeriod() {
        return period;
    }

    public void setPeriod(PayrollPeriod period) {
        this.period = period;
    }

    public Double getRegularPay() {
        return regularPay;
    }

    public void setRegularPay(Double regularPay) {
        this.regularPay = regularPay;
    }

    public Double getOvertimePay() {
        return overtimePay;
    }

    public void setOvertimePay(Double overtimePay) {
        this.overtimePay = overtimePay;
    }

    public int getWorkingDays() {
        return workingDays;
    }

    public void setWorkingDays(int workingDays) {
        this.workingDays = workingDays;
    }

    public Double getTotalHours() {
        return totalHours;
    }

    public void setTotalHours(Double totalHours) {
        this.totalHours = totalHours;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getOvertimeHours() {
        return overtimeHours;
    }

    public void setOvertimeHours(Double overtimeHours) {
        this.overtimeHours = overtimeHours;
    }

    public Double getTax() {
        return tax;
    }

    public void setTax(Double tax) {
        this.tax = tax;
    }

    public Double getBasicSalary() {
        return basicSalary;
    }

    public void setBasicSalary(Double basicSalary) {
        this.basicSalary = basicSalary;
    }

    public Double getIncomeTax() {
        return incomeTax;
    }

    public void setIncomeTax(Double incomeTax) {
        this.incomeTax = incomeTax;
    }

    public Double getGrossPay() {
        return grossPay;
    }

    public void setGrossPay(Double grossPay) {
        this.grossPay = grossPay;
    }

    public Double getNetPay() {
        return netPay;
    }

    public void setNetPay(Double netPay) {
        this.netPay = netPay;
    }

    public Double getSsnitEmployeeContribution() {
        return ssnitEmployeeContribution;
    }

    public void setSsnitEmployeeContribution(Double ssnitEmployeeContribution) {
        this.ssnitEmployeeContribution = ssnitEmployeeContribution;
    }

    public Double getSsnitEmployerContribution() {
        return ssnitEmployerContribution;
    }

    public void setSsnitEmployerContribution(Double ssnitEmployerContribution) {
        this.ssnitEmployerContribution = ssnitEmployerContribution;
    }

    public Double getTotalSsnitContribution() {
        return totalSsnitContribution;
    }

    public void setTotalSsnitContribution(Double totalSsnitContribution) {
        this.totalSsnitContribution = totalSsnitContribution;
    }

    public Double getTaxableIncome() {
        return taxableIncome;
    }

    public void setTaxableIncome(Double taxableIncome) {
        this.taxableIncome = taxableIncome;
    }

    public Double getOtherDeductions() {
        return otherDeductions;
    }

    public void setOtherDeductions(Double otherDeductions) {
        this.otherDeductions = otherDeductions;
    }

    public Double getGrossSalary() {
        return grossSalary;
    }

    public void setGrossSalary(Double grossSalary) {
        this.grossSalary = grossSalary;
    }

    public Double getSsnitEmployee() {
        return ssnitEmployee;
    }

    public void setSsnitEmployee(Double ssnitEmployee) {
        this.ssnitEmployee = ssnitEmployee;
    }

    public Double getSsnitEmployer() {
        return ssnitEmployer;
    }

    public void setSsnitEmployer(Double ssnitEmployer) {
        this.ssnitEmployer = ssnitEmployer;
    }

    public Double getPayeTax() {
        return payeTax;
    }

    public void setPayeTax(Double payeTax) {
        this.payeTax = payeTax;
    }

    public Double getNetSalary() {
        return netSalary;
    }

    public void setNetSalary(Double netSalary) {
        this.netSalary = netSalary;
    }

    public Double getRentAllowance() {
        return rentAllowance;
    }

    public Double getTransportAllowance() {
        return transportAllowance;
    }

    public Double getClothingAllowance() {
        return clothingAllowance;
    }

    public Double getOtherAllowance() {
        return otherAllowance;
    }

    public void setTotalAllowances(Double totalAllowances) {
        this.totalAllowances = totalAllowances;
    }

    public void setTaxableAllowances(Double taxableAllowances) {
        this.taxableAllowances = taxableAllowances;
    }

    public void setNonTaxableAllowances(Double nonTaxableAllowances) {
        this.nonTaxableAllowances = nonTaxableAllowances;
    }
}
