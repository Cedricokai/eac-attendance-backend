package com.example.eac_backend_project.entity.inventory;

import com.example.eac_backend_project.entity.attendance.Job;
import com.example.eac_backend_project.entity.attendance.RequestWorkflowInstance;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "inventory_request")
public class InventoryRequest {
    private Long requesterUserId;
    private String requesterEmail;
    private String requesterName;
    @Column(nullable=false, columnDefinition="boolean default false")
    private boolean financeFulfilment;


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_number", unique = true)
    private String requestNumber;

    @Column(name = "request_date")
    private LocalDateTime requestDate = LocalDateTime.now();

    @OneToOne(mappedBy = "inventoryRequest", cascade = CascadeType.ALL)
    private RequestWorkflowInstance workflowInstance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", referencedColumnName = "id")
    private Job job;

    @Column(name = "job_name")
    private String jobName;

    @Column(name = "cost_center_name")
    private String costCenterName;

    @Column(name = "cost_center_code")
    private String costCenterCode;

    // Many-to-One relationship with CostCenter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cost_center_id", referencedColumnName = "id")
    private CostCenter costCenter;


    @Column(name = "requested_by")
    private String requestedBy;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "department")
    private String department;

    @Column(name = "project_name")
    private String projectName;

    @Column(name = "job_description")
    private String jobDescription;

    @Column(name = "location")
    private String location;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "urgency")
    private String urgency; // "low", "normal", "high", "urgent"

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private RequestStatus status = RequestStatus.PROCUREMENT_PENDING;

    // Planner Approval
    @Column(name = "planner_approved_by")
    private String plannerApprovedBy;

    @Column(name = "planner_approval_date")
    private LocalDateTime plannerApprovalDate;

    @Column(name = "planner_notes")
    private String plannerNotes;

    // Procurement Approval
    @Column(name = "procurement_approved_by")
    private String procurementApprovedBy;

    @Column(name = "procurement_approval_date")
    private LocalDateTime procurementApprovalDate;

    @Column(name = "procurement_notes")
    private String procurementNotes;

    @Column(name = "estimated_cost")
    private Double estimatedCost;

    @Column(name = "delivery_date")
    private LocalDateTime deliveryDate;

    // Store Approval/Receipt
    @Column(name = "store_approved_by")
    private String storeApprovedBy;

    @Column(name = "store_approval_date")
    private LocalDateTime storeApprovalDate;

    @Column(name = "store_notes")
    private String storeNotes;

    @Column(name = "issued_quantity")
    private Integer issuedQuantity;

    @Column(name = "received_quantity")
    private Integer receivedQuantity;

    @Column(name = "received_date")
    private LocalDateTime receivedDate;

    @Column(name = "is_ppe_request")
    private Boolean isPpeRequest = false;

    @Column(name = "contact_person")
    private String contactPerson;

    @Column(name = "contact_phone")
    private String contactPhone;

    private LocalDateTime issueDate;

    private String issuedBy;

    @Column(name = "has_manual_products")
    private Boolean hasManualProducts = false;

    @OneToMany(mappedBy = "inventoryRequest", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JsonManagedReference
    private List<RequestItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (requestNumber == null) {
            String prefix = (isPpeRequest != null && isPpeRequest) ? "PPE-" : "REQ-";
            String datePart = String.valueOf(LocalDateTime.now().getYear());
            String sequence = String.format("%03d", (int)(Math.random() * 1000));
            requestNumber = prefix + datePart + "-" + sequence;
        }
        if (requestDate == null) {
            requestDate = LocalDateTime.now();
        }
        if (status == null) {
            // if PPE start procurement flow
            status = (isPpeRequest != null && isPpeRequest) ? RequestStatus.PROCUREMENT_PENDING : RequestStatus.PENDING;
        }
    }

    public Boolean getHasManualProducts() {
        return hasManualProducts;
    }

    public void setHasManualProducts(Boolean hasManualProducts) {
        this.hasManualProducts = hasManualProducts;
    }

    public LocalDateTime getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDateTime issueDate) {
        this.issueDate = issueDate;
    }

    public String getIssuedBy() {
        return issuedBy;
    }

    public void setIssuedBy(String issuedBy) {
        this.issuedBy = issuedBy;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRequestNumber() {
        return requestNumber;
    }

    public void setRequestNumber(String requestNumber) {
        this.requestNumber = requestNumber;
    }

    public LocalDateTime getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(LocalDateTime requestDate) {
        this.requestDate = requestDate;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
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

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public String getPlannerApprovedBy() {
        return plannerApprovedBy;
    }

    public void setPlannerApprovedBy(String plannerApprovedBy) {
        this.plannerApprovedBy = plannerApprovedBy;
    }

    public LocalDateTime getPlannerApprovalDate() {
        return plannerApprovalDate;
    }

    public void setPlannerApprovalDate(LocalDateTime plannerApprovalDate) {
        this.plannerApprovalDate = plannerApprovalDate;
    }

    public String getPlannerNotes() {
        return plannerNotes;
    }

    public void setPlannerNotes(String plannerNotes) {
        this.plannerNotes = plannerNotes;
    }

    public String getProcurementApprovedBy() {
        return procurementApprovedBy;
    }

    public void setProcurementApprovedBy(String procurementApprovedBy) {
        this.procurementApprovedBy = procurementApprovedBy;
    }

    public LocalDateTime getProcurementApprovalDate() {
        return procurementApprovalDate;
    }

    public void setProcurementApprovalDate(LocalDateTime procurementApprovalDate) {
        this.procurementApprovalDate = procurementApprovalDate;
    }

    public String getProcurementNotes() {
        return procurementNotes;
    }

    public void setProcurementNotes(String procurementNotes) {
        this.procurementNotes = procurementNotes;
    }

    public Double getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(Double estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    public LocalDateTime getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(LocalDateTime deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    public String getStoreApprovedBy() {
        return storeApprovedBy;
    }

    public void setStoreApprovedBy(String storeApprovedBy) {
        this.storeApprovedBy = storeApprovedBy;
    }

    public LocalDateTime getStoreApprovalDate() {
        return storeApprovalDate;
    }

    public void setStoreApprovalDate(LocalDateTime storeApprovalDate) {
        this.storeApprovalDate = storeApprovalDate;
    }

    public String getStoreNotes() {
        return storeNotes;
    }

    public void setStoreNotes(String storeNotes) {
        this.storeNotes = storeNotes;
    }

    public Integer getIssuedQuantity() {
        return issuedQuantity;
    }

    public void setIssuedQuantity(Integer issuedQuantity) {
        this.issuedQuantity = issuedQuantity;
    }

    public Integer getReceivedQuantity() {
        return receivedQuantity;
    }

    public void setReceivedQuantity(Integer receivedQuantity) {
        this.receivedQuantity = receivedQuantity;
    }

    public LocalDateTime getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(LocalDateTime receivedDate) {
        this.receivedDate = receivedDate;
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

    public List<RequestItem> getItems() {
        return items;
    }

    public void setItems(List<RequestItem> items) {
        this.items = items;
    }

    public void setIssuedDate(LocalDateTime now) {
        this.issueDate = now;
    }
}
