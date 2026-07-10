package com.example.eac_backend_project.entity.attendance;

import jakarta.persistence.*;

@Entity
@Table(name = "quote_items")
public class QuoteItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Double unitPrice;

    @Column(nullable = false)
    private Double total;

    // Constructors
    public QuoteItem() {}

    public QuoteItem(String description, Integer quantity, Double unitPrice) {
        this.description = description;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        calculateTotal();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
        calculateTotal();
    }

    public Double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Double unitPrice) {
        this.unitPrice = unitPrice;
        calculateTotal();
    }

    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }

    // Helper methods
    public void calculateTotal() {
        if (quantity != null && unitPrice != null) {
            this.total = quantity * unitPrice;
        }
    }
}