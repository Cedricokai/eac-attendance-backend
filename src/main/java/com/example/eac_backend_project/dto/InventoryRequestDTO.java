package com.example.eac_backend_project.dto;

import lombok.Data;
import java.util.List;

@Data
public class InventoryRequestDTO {

    private List<RequestItemDTO> items;
    private String requestedBy;
    private String department;
    private String projectName;
    private String jobDescription;
    private String location;
    private String notes;
    private String urgency;
    private Boolean isPpeRequest;
    private String contactPerson;
    private String contactPhone;
    private Boolean skipPlanner;
    private Boolean hasManualProducts;
    private String productName;

    private Long jobId;
    private String jobName;
    private Long costCenterId;
    private String costCenterName;
    private String costCenterCode;

    @Data
    public static class RequestItemDTO {
        private Long productId;
        private Integer quantity;
        private String notes;
        private Boolean isInventory;
        private String productName;
        private String productCode;  // Add this
        private String category;     // Add this
        private Double unitCost;     // Add this
        private Double totalCost;    // Add this
        private String estimatedUsage; // Add this
        private String status;       // Add this
        private Long jobId;
        private String jobName;
        private Long costCenterId;
        private String costCenterName;
        private String costCenterCode;

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }

        public Boolean getIsInventory() {
            return isInventory;
        }

        public void setIsInventory(Boolean inventory) {
            isInventory = inventory;
        }
    }

    public Boolean getHasManualProducts() {
        return hasManualProducts;
    }

    public void setHasManualProducts(Boolean hasManualProducts) {
        this.hasManualProducts = hasManualProducts;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Boolean getSkipPlanner() {
        return skipPlanner;
    }

    public void setSkipPlanner(Boolean skipPlanner) {
        this.skipPlanner = skipPlanner;
    }

    public List<RequestItemDTO> getItems() {
        return items;
    }

    public void setItems(List<RequestItemDTO> items) {
        this.items = items;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getUrgency() {
        return urgency;
    }

    public void setUrgency(String urgency) {
        this.urgency = urgency;
    }

    public Boolean getPpeRequest() {
        return isPpeRequest;
    }

    public void setPpeRequest(Boolean ppeRequest) {
        isPpeRequest = ppeRequest;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }
}