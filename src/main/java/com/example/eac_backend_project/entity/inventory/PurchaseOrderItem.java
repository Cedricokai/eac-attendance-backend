package com.example.eac_backend_project.entity.inventory;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "purchase_order_items")
public class PurchaseOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id")
    @JsonBackReference
    private PurchaseOrder purchaseOrder;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "product_code")
    private String productCode;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "unit_price")
    private Double unitPrice;

    @Column(name = "total_price")
    private Double totalPrice;

    @Column(name = "is_new_product")
    private Boolean isNewProduct = false;

    @Column(name = "received_quantity")
    private Integer receivedQuantity = 0;

    @Column(name = "status")
    private String status = "ORDERED"; // ORDERED, DELIVERED, RECEIVED
}