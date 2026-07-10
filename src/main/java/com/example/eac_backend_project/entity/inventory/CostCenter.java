// CostCenter.java
package com.example.eac_backend_project.entity.inventory;

import com.example.eac_backend_project.entity.attendance.Job;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cost_centers")
@Data
public class CostCenter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    private CostCenterType type;

    @Enumerated(EnumType.STRING)
    private CostCenterStatus status = CostCenterStatus.ACTIVE;

    @Column(nullable = false)
    private Double budget = 0.0;

    @Column(nullable = false)
    private Double spentAmount = 0.0;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @OneToMany(mappedBy = "costCenter", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CostCenterProduct> products = new ArrayList<>();

    // Link to Job (One-to-One relationship)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "costCenter", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CostTransaction> transactions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getJobId() {
        return job != null ? job.getId() : null;
    }


    // Helper methods
    public Double getRemainingBudget() {
        return budget - spentAmount;
    }

    public Double getUtilizationPercentage() {
        if (budget <= 0) return 0.0;
        return (spentAmount / budget) * 100;
    }

    public void addSpentAmount(Double amount) {
        this.spentAmount += amount;
    }

    public void deductSpentAmount(Double amount) {
        this.spentAmount -= amount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<CostCenterProduct> getProducts() {
        return products;
    }

    public void setProducts(List<CostCenterProduct> products) {
        this.products = products;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public CostCenterType getType() {
        return type;
    }

    public void setType(CostCenterType type) {
        this.type = type;
    }

    public CostCenterStatus getStatus() {
        return status;
    }

    public void setStatus(CostCenterStatus status) {
        this.status = status;
    }

    public Double getBudget() {
        return budget;
    }

    public void setBudget(Double budget) {
        this.budget = budget;
    }

    public Double getSpentAmount() {
        return spentAmount;
    }

    public void setSpentAmount(Double spentAmount) {
        this.spentAmount = spentAmount;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<CostTransaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<CostTransaction> transactions) {
        this.transactions = transactions;
    }
}

