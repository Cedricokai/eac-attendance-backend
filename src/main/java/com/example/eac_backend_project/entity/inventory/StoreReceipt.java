package com.example.eac_backend_project.entity.inventory;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "store_receipts")
public class StoreReceipt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private PurchaseOrder purchaseOrder;

    @ManyToOne
    private PurchaseOrderItem item;

    private Integer goodQuantity;
    private Integer damagedQuantity;
    private String receivedBy;
    private String notes;
    private LocalDateTime receivedDate;
    private Boolean stockUpdated = false;
}