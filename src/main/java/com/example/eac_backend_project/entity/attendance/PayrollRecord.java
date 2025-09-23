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

    private Double rentAllowance;
    private Double transportAllowance;  // TnT
    private Double clothingAllowance;
    private Double otherAllowance;
    private Double totalAllowances;
    private Double taxableAllowances;
    private Double nonTaxableAllowances;


    public Double getRentAllowance() {
        return rentAllowance;
    }

    public void setRentAllowance(Double rentAllowance) {
        this.rentAllowance = rentAllowance;
    }

    public Double getTransportAllowance() {
        return transportAllowance;
    }

    public void setTransportAllowance(Double transportAllowance) {
        this.transportAllowance = transportAllowance;
    }

    public Double getClothingAllowance() {
        return clothingAllowance;
    }

    public void setClothingAllowance(Double clothingAllowance) {
        this.clothingAllowance = clothingAllowance;
    }

    public Double getOtherAllowance() {
        return otherAllowance;
    }

    public void setOtherAllowance(Double otherAllowance) {
        this.otherAllowance = otherAllowance;
    }

    public Double getTotalAllowances() {
        return totalAllowances;
    }

    public void setTotalAllowances(Double totalAllowances) {
        this.totalAllowances = totalAllowances;
    }

    public Double getTaxableAllowances() {
        return taxableAllowances;
    }

    public void setTaxableAllowances(Double taxableAllowances) {
        this.taxableAllowances = taxableAllowances;
    }

    public Double getNonTaxableAllowances() {
        return nonTaxableAllowances;
    }

    public void setNonTaxableAllowances(Double nonTaxableAllowances) {
        this.nonTaxableAllowances = nonTaxableAllowances;
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

    public Double getTax() {
        return tax;
    }

    public void setTax(Double tax) {
        this.tax = tax;
    }

    // Safe getter methods to prevent null pointer exceptions
    public Double getOvertimeHours() {
        return overtimeHours != null ? overtimeHours : 0.0;
    }

    public Double getRegularPay() {
        return regularPay != null ? regularPay : 0.0;
    }

    public Double getOvertimePay() {
        return overtimePay != null ? overtimePay : 0.0;
    }

    public Double getGrossPay() {
        return grossPay != null ? grossPay : 0.0;
    }

    // Similarly safe setters
    public void setOvertimeHours(Double overtimeHours) {
        this.overtimeHours = overtimeHours != null ? overtimeHours : 0.0;
    }

    public void setRegularPay(Double regularPay) {
        this.regularPay = regularPay != null ? regularPay : 0.0;
    }

    public void setOvertimePay(Double overtimePay) {
        this.overtimePay = overtimePay != null ? overtimePay : 0.0;
    }

    public void setGrossPay(Double grossPay) {
        this.grossPay = grossPay != null ? grossPay : 0.0;
    }

    // Getters and setters for other fields...
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }

    public PayrollPeriod getPeriod() { return period; }
    public void setPeriod(PayrollPeriod period) { this.period = period; }

    public int getWorkingDays() { return workingDays; }
    public void setWorkingDays(int workingDays) { this.workingDays = workingDays; }

    public Double getTotalHours() { return totalHours != null ? totalHours : 0.0; }
    public void setTotalHours(Double totalHours) { this.totalHours = totalHours != null ? totalHours : 0.0; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getBasicSalary() { return basicSalary != null ? basicSalary : 0.0; }
    public void setBasicSalary(Double basicSalary) { this.basicSalary = basicSalary != null ? basicSalary : 0.0; }

    public Double getIncomeTax() { return incomeTax != null ? incomeTax : 0.0; }
    public void setIncomeTax(Double incomeTax) { this.incomeTax = incomeTax != null ? incomeTax : 0.0; }

    public Double getNetPay() { return netPay != null ? netPay : 0.0; }
    public void setNetPay(Double netPay) { this.netPay = netPay != null ? netPay : 0.0; }

    public Double getSsnitEmployeeContribution() { return ssnitEmployeeContribution != null ? ssnitEmployeeContribution : 0.0; }
    public void setSsnitEmployeeContribution(Double ssnitEmployeeContribution) { this.ssnitEmployeeContribution = ssnitEmployeeContribution != null ? ssnitEmployeeContribution : 0.0; }

    public Double getSsnitEmployerContribution() { return ssnitEmployerContribution != null ? ssnitEmployerContribution : 0.0; }
    public void setSsnitEmployerContribution(Double ssnitEmployerContribution) { this.ssnitEmployerContribution = ssnitEmployerContribution != null ? ssnitEmployerContribution : 0.0; }

    public Double getTotalSsnitContribution() { return totalSsnitContribution != null ? totalSsnitContribution : 0.0; }
    public void setTotalSsnitContribution(Double totalSsnitContribution) { this.totalSsnitContribution = totalSsnitContribution != null ? totalSsnitContribution : 0.0; }

    public Double getTaxableIncome() { return taxableIncome != null ? taxableIncome : 0.0; }
    public void setTaxableIncome(Double taxableIncome) { this.taxableIncome = taxableIncome != null ? taxableIncome : 0.0; }

    public Double getOtherDeductions() { return otherDeductions != null ? otherDeductions : 0.0; }
    public void setOtherDeductions(Double otherDeductions) { this.otherDeductions = otherDeductions != null ? otherDeductions : 0.0; }
}