package com.example.eac_backend_project.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class StoreReceiptDTO {

    @NotNull(message = "Purchase order ID is required")
    private Long purchaseOrderId;

    @NotNull(message = "Item ID is required")
    private Long itemId;

    @NotNull(message = "Good quantity is required")
    @Min(value = 0, message = "Good quantity must be 0 or greater")
    private Integer goodQuantity;

    @NotNull(message = "Damaged quantity is required")
    @Min(value = 0, message = "Damaged quantity must be 0 or greater")
    private Integer damagedQuantity;

    @NotBlank(message = "Received by is required")
    private String receivedBy;

    private String notes;

    private LocalDateTime receivedDate;

    // Constructors
    public StoreReceiptDTO() {}

    public StoreReceiptDTO(Long purchaseOrderId, Long itemId, Integer goodQuantity,
                           Integer damagedQuantity, String receivedBy, String notes) {
        this.purchaseOrderId = purchaseOrderId;
        this.itemId = itemId;
        this.goodQuantity = goodQuantity;
        this.damagedQuantity = damagedQuantity;
        this.receivedBy = receivedBy;
        this.notes = notes;
        this.receivedDate = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getPurchaseOrderId() {
        return purchaseOrderId;
    }

    public void setPurchaseOrderId(Long purchaseOrderId) {
        this.purchaseOrderId = purchaseOrderId;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Integer getGoodQuantity() {
        return goodQuantity;
    }

    public void setGoodQuantity(Integer goodQuantity) {
        this.goodQuantity = goodQuantity;
    }

    public Integer getDamagedQuantity() {
        return damagedQuantity;
    }

    public void setDamagedQuantity(Integer damagedQuantity) {
        this.damagedQuantity = damagedQuantity;
    }

    public String getReceivedBy() {
        return receivedBy;
    }

    public void setReceivedBy(String receivedBy) {
        this.receivedBy = receivedBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(LocalDateTime receivedDate) {
        this.receivedDate = receivedDate;
    }

    // Helper method to get total quantity
    public Integer getTotalQuantity() {
        return (goodQuantity != null ? goodQuantity : 0) +
                (damagedQuantity != null ? damagedQuantity : 0);
    }

    // Validation method
    public boolean isValidQuantity() {
        return goodQuantity != null && goodQuantity >= 0 &&
                damagedQuantity != null && damagedQuantity >= 0 &&
                getTotalQuantity() > 0;
    }
}