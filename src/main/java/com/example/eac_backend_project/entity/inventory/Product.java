package com.example.eac_backend_project.entity.inventory;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    private String name;

    @Column(name = "product_code", nullable = true)
    private String code;

    @Column(name = "shelf_number")
    private String shelfNumber;

    @Column(name = "ppe")
    private Boolean ppe;

    @Column(name = "kitchen_tools")
    private Boolean kitchenStore;

    private String description;

    private int stock;
    private String userName;

    @Column(name = "product_type")
    private String productType;

    @Column(name = "cost")
    private Double unitCost;

    @Column(name = "created_at", updatable = false)
    private LocalDate createdDate;

    @Column(name = "last_updated_date")
    private LocalDateTime lastUpdatedDate;

    @Column(name = "entry_date")
    private LocalDateTime entryDate;

    @Column(name = "returnable_after_use")
    private Boolean returnableAfterUse;

    // ✅ FIX 1: Update timestamp when stock changes
    public void setStock(int stock) {
        this.stock = stock;
        this.lastUpdatedDate = LocalDateTime.now();
    }

    // ✅ FIX 2: Add constructor that sets timestamps
    public Product() {
        this.lastUpdatedDate = LocalDateTime.now();
        if (this.entryDate == null) {
            this.entryDate = LocalDateTime.now();
        }
    }

    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDate.now();
        }
        if (lastUpdatedDate == null) {
            lastUpdatedDate = LocalDateTime.now();
        }
        if (entryDate == null) {
            entryDate = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdatedDate = LocalDateTime.now();
    }

    public Boolean getKitchenStore() {
        return kitchenStore;
    }

    public void setKitchenStore(Boolean kitchenStore) {
        this.kitchenStore = kitchenStore;
    }

    public Double getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(Double unitCost) {
        this.unitCost = unitCost;
    }

    // Keep all other getters and setters as they were...
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.lastUpdatedDate = LocalDateTime.now();
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
        this.lastUpdatedDate = LocalDateTime.now();
    }

    public Boolean getPpe() {
        return ppe;
    }

    public void setPpe(Boolean ppe) {
        this.ppe = ppe;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.lastUpdatedDate = LocalDateTime.now();
    }

    public int getStock() {
        return stock;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
        this.lastUpdatedDate = LocalDateTime.now();
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
        this.lastUpdatedDate = LocalDateTime.now();
    }

    public LocalDate getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDate createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    public void setLastUpdatedDate(LocalDateTime lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }

    public LocalDateTime getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(LocalDateTime entryDate) {
        this.entryDate = entryDate;
        this.lastUpdatedDate = LocalDateTime.now();
    }

    public void setReturnableAfterUse(Boolean returnableAfterUse) {
        this.returnableAfterUse = returnableAfterUse;
    }

    public Boolean getReturnableAfterUse() {
        return returnableAfterUse;
    }

    // Optional: Helper method for string compatibility
    public void setPpe(String ppe) {
        this.ppe = "true".equalsIgnoreCase(ppe) || "yes".equalsIgnoreCase(ppe);
        this.lastUpdatedDate = LocalDateTime.now();
    }
}