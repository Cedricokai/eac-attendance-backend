// MonthlyReportResponseDTO.java
package com.example.eac_backend_project.dto;

import java.util.List;
import java.util.Map;

public class MonthlyReportResponseDTO {
    private List<String> availableMonths;
    private List<String> availableCategories;
    private Map<String, Map<String, CategoryMonthReportDTO.MonthlyTotals>> reportData;
    private Map<String, Double> grandTotals;

    public MonthlyReportResponseDTO() {}

    public List<String> getAvailableMonths() { return availableMonths; }
    public void setAvailableMonths(List<String> availableMonths) { this.availableMonths = availableMonths; }
    public List<String> getAvailableCategories() { return availableCategories; }
    public void setAvailableCategories(List<String> availableCategories) { this.availableCategories = availableCategories; }
    public Map<String, Map<String, CategoryMonthReportDTO.MonthlyTotals>> getReportData() { return reportData; }
    public void setReportData(Map<String, Map<String, CategoryMonthReportDTO.MonthlyTotals>> reportData) { this.reportData = reportData; }
    public Map<String, Double> getGrandTotals() { return grandTotals; }
    public void setGrandTotals(Map<String, Double> grandTotals) { this.grandTotals = grandTotals; }
}