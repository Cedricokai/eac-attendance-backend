package com.example.eac_backend_project.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ppe_items")
public class PPE {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String code;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private Integer stock = 0;

    @Column(name = "brand")
    private String brand;

    @Column(name = "ppe_type")
    private String ppeType; // Helmet, Gloves, Safety Glasses, etc.

    @Column(name = "size")
    private String size; // S, M, L, XL, etc.

    @Column(name = "material")
    private String material;

    @Column(name = "color")
    private String color;

    @Column(name = "cost")
    private Double cost;

    @Column(name = "location")
    private String location;

    @Column(name = "safety_standard")
    private String safetyStandard; // ANSI, OSHA, CE, etc.

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @Column(name = "last_inspection_date")
    private LocalDateTime lastInspectionDate;

    @Column(name = "next_inspection_date")
    private LocalDateTime nextInspectionDate;

    @Column(name = "entry_date")
    private LocalDateTime entryDate;

    @Column(name = "created_date")
    private LocalDateTime createdDate = LocalDateTime.now();

    @Column(name = "min_stock_level")
    private Integer minStockLevel = 0;

    @Column(name = "is_active")
    private Boolean isActive = true;

    // Constructors
    public PPE() {
    }

    public PPE(String name, String description, Integer stock, String brand, String ppeType,
               String size, String material, String color, Double cost, String location) {
        this.name = name;
        this.description = description;
        this.stock = stock;
        this.brand = brand;
        this.ppeType = ppeType;
        this.size = size;
        this.material = material;
        this.color = color;
        this.cost = cost;
        this.location = location;
        this.createdDate = LocalDateTime.now();
        this.entryDate = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getPpeType() { return ppeType; }
    public void setPpeType(String ppeType) { this.ppeType = ppeType; }

    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }

    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public Double getCost() { return cost; }
    public void setCost(Double cost) { this.cost = cost; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getSafetyStandard() { return safetyStandard; }
    public void setSafetyStandard(String safetyStandard) { this.safetyStandard = safetyStandard; }

    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }

    public LocalDateTime getLastInspectionDate() { return lastInspectionDate; }
    public void setLastInspectionDate(LocalDateTime lastInspectionDate) { this.lastInspectionDate = lastInspectionDate; }

    public LocalDateTime getNextInspectionDate() { return nextInspectionDate; }
    public void setNextInspectionDate(LocalDateTime nextInspectionDate) { this.nextInspectionDate = nextInspectionDate; }

    public LocalDateTime getEntryDate() { return entryDate; }
    public void setEntryDate(LocalDateTime entryDate) { this.entryDate = entryDate; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public Integer getMinStockLevel() { return minStockLevel; }
    public void setMinStockLevel(Integer minStockLevel) { this.minStockLevel = minStockLevel; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}