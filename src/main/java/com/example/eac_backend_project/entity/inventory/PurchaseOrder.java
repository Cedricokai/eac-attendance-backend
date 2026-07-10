package com.example.eac_backend_project.entity.inventory;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "purchase_orders")
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "purchase_order_number", unique = true)
    private String purchaseOrderNumber;

    @Column(name = "supplier_name")
    private String supplierName;

    @Column(name = "supplier_contact")
    private String supplierContact;

    @Column(name = "order_date")
    private LocalDateTime orderDate;

    @Column(name = "expected_delivery_date")
    private LocalDateTime expectedDeliveryDate;

    @Column(name = "delivery_address")
    private String deliveryAddress;

    @Column(name = "total_amount")
    private Double totalAmount;

    @Column(name = "status")
    private String status = "PENDING"; // PENDING, ORDERED, DELIVERED, RECEIVED

    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "ordered_by")
    private String orderedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "store_notified")
    private Boolean storeNotified = false;

    @Column(name = "store_notification_date")
    private LocalDateTime storeNotificationDate;


    @OneToMany(
            mappedBy = "purchaseOrder",
            cascade = CascadeType.ALL,
            fetch = FetchType.EAGER,
            orphanRemoval = true
    )
    @JsonManagedReference
    private List<PurchaseOrderItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (purchaseOrderNumber == null) {
            purchaseOrderNumber = "PO-" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) +
                    "-" + String.format("%04d", (int)(Math.random() * 10000));
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}