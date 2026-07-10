package com.example.eac_backend_project.entity.attendance;

import jakarta.persistence.*;


import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quotes")
public class Quote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String quoteNumber;

    @Column(nullable = false)
    private LocalDate quoteDate;

    @Column(nullable = false)
    private LocalDate validUntil;

    @Column(nullable = false)
    private String customerName;

    private String customerEmail;
    private String customerPhone;

    @Column(length = 500)
    private String customerAddress;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "quote_id")
    private List<QuoteItem> items = new ArrayList<>();

    private Double subtotal;
    private Double taxRate;
    private Double taxAmount;
    private Double discount;
    private Double total;

    @Column(length = 1000)
    private String notes;

    @Column(length = 1000)
    private String terms;

    @Enumerated(EnumType.STRING)
    private QuoteStatus status;

    @Column(updatable = false)
    private LocalDate createdAt;

    private LocalDate updatedAt;

    // Enums
    public enum QuoteStatus {
        DRAFT, SENT, ACCEPTED, REJECTED, EXPIRED
    }

    // Constructors
    public Quote() {
        this.createdAt = LocalDate.now();
        this.updatedAt = LocalDate.now();
        this.status = QuoteStatus.DRAFT;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getQuoteNumber() { return quoteNumber; }
    public void setQuoteNumber(String quoteNumber) { this.quoteNumber = quoteNumber; }

    public LocalDate getQuoteDate() { return quoteDate; }
    public void setQuoteDate(LocalDate quoteDate) { this.quoteDate = quoteDate; }

    public LocalDate getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDate validUntil) { this.validUntil = validUntil; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getCustomerAddress() { return customerAddress; }
    public void setCustomerAddress(String customerAddress) { this.customerAddress = customerAddress; }

    public List<QuoteItem> getItems() { return items; }
    public void setItems(List<QuoteItem> items) { this.items = items; }

    public Double getSubtotal() { return subtotal; }
    public void setSubtotal(Double subtotal) { this.subtotal = subtotal; }

    public Double getTaxRate() { return taxRate; }
    public void setTaxRate(Double taxRate) { this.taxRate = taxRate; }

    public Double getTaxAmount() { return taxAmount; }
    public void setTaxAmount(Double taxAmount) { this.taxAmount = taxAmount; }

    public Double getDiscount() { return discount; }
    public void setDiscount(Double discount) { this.discount = discount; }

    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getTerms() { return terms; }
    public void setTerms(String terms) { this.terms = terms; }

    public QuoteStatus getStatus() { return status; }
    public void setStatus(QuoteStatus status) { this.status = status; }

    public LocalDate getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }

    public LocalDate getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDate updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDate.now();
    }

    // Helper methods
    public void addItem(QuoteItem item) {
        items.add(item);
    }

    public void removeItem(QuoteItem item) {
        items.remove(item);
    }

    public void calculateTotals() {
        if (items != null) {
            this.subtotal = items.stream()
                    .mapToDouble(QuoteItem::getTotal)
                    .sum();

            this.taxAmount = this.subtotal * (this.taxRate != null ? this.taxRate / 100 : 0);
            this.total = this.subtotal + this.taxAmount - (this.discount != null ? this.discount : 0);
        }
    }
}