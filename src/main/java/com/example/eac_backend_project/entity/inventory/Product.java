package com.example.eac_backend_project.entity.inventory;

import com.example.eac_backend_project.Service.ProductService;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "product")
public class Product {

    @Autowired
    private transient ProductService productService;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    private String name;
    @Column(name = "product_code", nullable = true) // Nullable is default if not explicitly set
    private String code;
    
    private String description;
    private int stock;
    private String userName;

    @Column(name = "product_type")
    private String productType;
   
    @Column(name = "created_at", updatable = false) 
    private LocalDate createdDate;          // Store the creation date
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
        lastUpdatedDate = LocalDateTime.now();   // Set current date and time on every update
    }

    public ProductService getProductService() {
        return productService;
    }

    public void setProductService(ProductService productService) {
        this.productService = productService;
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
