package com.example.eac_backend_project.dto;

import java.util.List;
import java.util.Map;

public class LoanPreviewDTO {
    private Long employeeId;
    private String employeeName;
    private List<LoanDetail> loans;
    private double totalMonthlyDeduction;
    private double totalRemainingBalance;

    public LoanPreviewDTO() {}

    // getters and setters
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public List<LoanDetail> getLoans() { return loans; }
    public void setLoans(List<LoanDetail> loans) { this.loans = loans; }
    public double getTotalMonthlyDeduction() { return totalMonthlyDeduction; }
    public void setTotalMonthlyDeduction(double totalMonthlyDeduction) { this.totalMonthlyDeduction = totalMonthlyDeduction; }
    public double getTotalRemainingBalance() { return totalRemainingBalance; }
    public void setTotalRemainingBalance(double totalRemainingBalance) { this.totalRemainingBalance = totalRemainingBalance; }

    public static class LoanDetail {
        private Long loanId;
        private double monthlyPayment;
        private double remainingBalance;
        private boolean isConsolidated;
        private double originalAmount;
        private double paidToDate;

        // getters and setters
        public Long getLoanId() { return loanId; }
        public void setLoanId(Long loanId) { this.loanId = loanId; }
        public double getMonthlyPayment() { return monthlyPayment; }
        public void setMonthlyPayment(double monthlyPayment) { this.monthlyPayment = monthlyPayment; }
        public double getRemainingBalance() { return remainingBalance; }
        public void setRemainingBalance(double remainingBalance) { this.remainingBalance = remainingBalance; }
        public boolean isConsolidated() { return isConsolidated; }
        public void setConsolidated(boolean consolidated) { isConsolidated = consolidated; }
        public double getOriginalAmount() { return originalAmount; }
        public void setOriginalAmount(double originalAmount) { this.originalAmount = originalAmount; }
        public double getPaidToDate() { return paidToDate; }
        public void setPaidToDate(double paidToDate) { this.paidToDate = paidToDate; }
    }
}