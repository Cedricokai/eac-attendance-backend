package com.example.eac_backend_project.entity.inventory;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "cost_center_products",
        uniqueConstraints = @UniqueConstraint(columnNames = {"cost_center_id", "product_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CostCenterProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cost_center_id", nullable = false)
    @JsonBackReference
    private CostCenter costCenter;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "requested_quantity", nullable = false)
    private Integer requestedQuantity = 0;

    @Column(name = "approved_quantity")
    private Integer approvedQuantity = 0;

    @Column(name = "issued_quantity")
    private Integer issuedQuantity = 0;

    @Column(name = "estimated_usage", length = 500)
    private String estimatedUsage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CostCenterProductStatus status = CostCenterProductStatus.PROCUREMENT_PENDING;

    @Column(name = "unit_cost")
    private Double unitCost;

    @Column(name = "total_cost")
    private Double totalCost;

    @Column(name = "request_date")
    private LocalDateTime requestDate;

    @Column(name = "approval_date")
    private LocalDateTime approvalDate;

    @Column(name = "issue_date")
    private LocalDateTime issueDate;

    @Column(length = 1000)
    private String remarks;

    @ManyToOne
    @JoinColumn(name = "inventory_request_id")
    private InventoryRequest inventoryRequest;

    @PrePersist
    protected void onCreate() {
        requestDate = LocalDateTime.now();
        if (unitCost == null && product != null) {
            unitCost = product.getUnitCost();
        }
        calculateTotalCost();
    }

    @PreUpdate
    protected void onUpdate() {
        calculateTotalCost();
    }

    public void calculateTotalCost() {
        if (unitCost != null && requestedQuantity != null) {
            totalCost = unitCost * requestedQuantity;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CostCenter getCostCenter() {
        return costCenter;
    }

    public void setCostCenter(CostCenter costCenter) {
        this.costCenter = costCenter;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Integer getRequestedQuantity() {
        return requestedQuantity;
    }

    public void setRequestedQuantity(Integer requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
    }

    public Integer getApprovedQuantity() {
        return approvedQuantity;
    }

    public void setApprovedQuantity(Integer approvedQuantity) {
        this.approvedQuantity = approvedQuantity;
    }

    public Integer getIssuedQuantity() {
        return issuedQuantity;
    }

    public void setIssuedQuantity(Integer issuedQuantity) {
        this.issuedQuantity = issuedQuantity;
    }

    public String getEstimatedUsage() {
        return estimatedUsage;
    }

    public void setEstimatedUsage(String estimatedUsage) {
        this.estimatedUsage = estimatedUsage;
    }

    public CostCenterProductStatus getStatus() {
        return status;
    }

    public void setStatus(CostCenterProductStatus status) {
        this.status = status;
    }

    public Double getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(Double unitCost) {
        this.unitCost = unitCost;
    }

    public Double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(Double totalCost) {
        this.totalCost = totalCost;
    }

    public LocalDateTime getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(LocalDateTime requestDate) {
        this.requestDate = requestDate;
    }

    public LocalDateTime getApprovalDate() {
        return approvalDate;
    }

    public void setApprovalDate(LocalDateTime approvalDate) {
        this.approvalDate = approvalDate;
    }

    public LocalDateTime getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDateTime issueDate) {
        this.issueDate = issueDate;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}