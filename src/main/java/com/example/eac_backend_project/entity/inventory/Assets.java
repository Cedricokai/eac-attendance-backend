package com.example.eac_backend_project.config;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "assets")  // Optional if table name is not the same as the class name
public class Assets {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "asset_id")  // Ensure this matches the column name in the DB
    private Long id;

    @Column(name = "product_name")  // Map to the correct column name in the database
    private String name;

    @Column(name = "product_code")  // Map to the correct column name in the database
    private String code;

    private String description;

    @Column(name = "stock")  // Assuming stock is stored as an integer in your DB
    private int stock;


    @Column(name = "userName")
    private String userName;

    @Column(name = "product_type")
    private String productType;

    @Column(name = "created_at", updatable = false)  // Map to the correct column and prevent updates
    private LocalDate createdDate;  // Store the creation date

    @Column(name = "updated_at")
    private LocalDateTime lastUpdatedDate;  // Store the last update timestamp

    // Automatically set createdDate before persisting the entity
    @PrePersist
    protected void onCreate() {
        createdDate = LocalDate.now();           // Set current date at creation
        lastUpdatedDate = LocalDateTime.now();   // Set current date and time at creation
    }

    // Automatically update lastUpdatedDate before updating the entity
    @PreUpdate
    protected void onUpdate() {
        lastUpdatedDate = LocalDateTime.now();
    }

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
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
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
}
