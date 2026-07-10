package com.example.eac_backend_project.dto;

import java.util.ArrayList;
import java.util.List;

public class LoanReversalResultDTO {
    private boolean success;
    private String message;
    private int totalRecordsProcessed;
    private int totalRecordsReversed;
    private List<ReversalDetail> details = new ArrayList<>();

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getTotalRecordsProcessed() {
        return totalRecordsProcessed;
    }

    public void setTotalRecordsProcessed(int totalRecordsProcessed) {
        this.totalRecordsProcessed = totalRecordsProcessed;
    }

    public int getTotalRecordsReversed() {
        return totalRecordsReversed;
    }

    public void setTotalRecordsReversed(int totalRecordsReversed) {
        this.totalRecordsReversed = totalRecordsReversed;
    }

    public List<ReversalDetail> getDetails() {
        return details;
    }

    public void setDetails(List<ReversalDetail> details) {
        this.details = details;
    }

    public static class ReversalDetail {
        private Long employeeId;
        private String employeeName;
        private Double amountReversed;
        private Double originalNetSalary;
        private Double restoredNetSalary;
        private String status;

        // Getters and Setters
        public Long getEmployeeId() {
            return employeeId;
        }

        public void setEmployeeId(Long employeeId) {
            this.employeeId = employeeId;
        }

        public String getEmployeeName() {
            return employeeName;
        }

        public void setEmployeeName(String employeeName) {
            this.employeeName = employeeName;
        }

        public Double getAmountReversed() {
            return amountReversed;
        }

        public void setAmountReversed(Double amountReversed) {
            this.amountReversed = amountReversed;
        }

        public Double getOriginalNetSalary() {
            return originalNetSalary;
        }

        public void setOriginalNetSalary(Double originalNetSalary) {
            this.originalNetSalary = originalNetSalary;
        }

        public Double getRestoredNetSalary() {
            return restoredNetSalary;
        }

        public void setRestoredNetSalary(Double restoredNetSalary) {
            this.restoredNetSalary = restoredNetSalary;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}