// CategoryMonthReportDTO.java
package com.example.eac_backend_project.dto;

import java.util.Map;

public class CategoryMonthReportDTO {
    private String category;
    private Map<String, MonthlyTotals> monthlyTotals;

    public CategoryMonthReportDTO(String category, Map<String, MonthlyTotals> monthlyTotals) {
        this.category = category;
        this.monthlyTotals = monthlyTotals;
    }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Map<String, MonthlyTotals> getMonthlyTotals() { return monthlyTotals; }
    public void setMonthlyTotals(Map<String, MonthlyTotals> monthlyTotals) { this.monthlyTotals = monthlyTotals; }

    public static class MonthlyTotals {
        private double totalNetSalary;
        private double totalTax;
        private double totalSsnitEmployee;
        private double totalSsnitEmployer;
        private double totalGrossSalary;
        private int employeeCount;
        private String periodId;
        private String periodName;

        public MonthlyTotals() {}

        public double getTotalNetSalary() { return totalNetSalary; }
        public void setTotalNetSalary(double totalNetSalary) { this.totalNetSalary = totalNetSalary; }
        public double getTotalTax() { return totalTax; }
        public void setTotalTax(double totalTax) { this.totalTax = totalTax; }
        public double getTotalSsnitEmployee() { return totalSsnitEmployee; }
        public void setTotalSsnitEmployee(double totalSsnitEmployee) { this.totalSsnitEmployee = totalSsnitEmployee; }
        public double getTotalSsnitEmployer() { return totalSsnitEmployer; }
        public void setTotalSsnitEmployer(double totalSsnitEmployer) { this.totalSsnitEmployer = totalSsnitEmployer; }
        public double getTotalGrossSalary() { return totalGrossSalary; }
        public void setTotalGrossSalary(double totalGrossSalary) { this.totalGrossSalary = totalGrossSalary; }
        public int getEmployeeCount() { return employeeCount; }
        public void setEmployeeCount(int employeeCount) { this.employeeCount = employeeCount; }
        public String getPeriodId() { return periodId; }
        public void setPeriodId(String periodId) { this.periodId = periodId; }
        public String getPeriodName() { return periodName; }
        public void setPeriodName(String periodName) { this.periodName = periodName; }

        public double getTotalSsnit() {
            return totalSsnitEmployee + totalSsnitEmployer;
        }
    }
}