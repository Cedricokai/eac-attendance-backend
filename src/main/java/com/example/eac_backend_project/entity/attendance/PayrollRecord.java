package com.example.eac_backend_project.entity.attendance;

import jakarta.persistence.*;
import lombok.Data;

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

    private Double regularPay;
    private Double overtimePay;
    private int workingDays;
    private Double totalHours;
    private String status; // "Generated", "Processed"
    private Double overtimeHours;

    private Double tax;

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

    // ===== Allowances (split + totals) =====
    private Double rentAllowance = 0.0;
    private Double transportAllowance = 0.0;  // TnT
    private Double clothingAllowance = 0.0;
    private Double otherAllowance = 0.0;

    private Double totalAllowances = 0.0;
    private Double taxableAllowances = 0.0;
    private Double nonTaxableAllowances = 0.0;

    // ================== Helpers ==================
    private Double safe(Double value) {
        return value != null ? value : 0.0;
    }

    private void recalcAllowances() {
        this.totalAllowances = safe(rentAllowance) + safe(transportAllowance) +
                safe(clothingAllowance) + safe(otherAllowance);
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
