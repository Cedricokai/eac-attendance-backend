package com.example.eac_backend_project.entity.attendance;

import com.example.eac_backend_project.entity.inventory.Product;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_products")
public class JobProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "estimated_usage", columnDefinition = "TEXT")
    private String estimatedUsage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public JobProduct() {}

    public JobProduct(Job job, Product product, Integer quantity) {
        this.job = job;
        this.product = product;
        this.quantity = quantity;
    }

    public JobProduct(Job job, Product product, Integer quantity, String estimatedUsage) {
        this.job = job;
        this.product = product;
        this.quantity = quantity;
        this.estimatedUsage = estimatedUsage;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Job getJob() { return job; }
    public void setJob(Job job) { this.job = job; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getEstimatedUsage() { return estimatedUsage; }
    public void setEstimatedUsage(String estimatedUsage) { this.estimatedUsage = estimatedUsage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}