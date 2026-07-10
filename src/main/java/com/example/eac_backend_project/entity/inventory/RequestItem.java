package com.example.eac_backend_project.entity.inventory;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "request_items")
@Data
public class RequestItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id")
    @JsonBackReference
    private InventoryRequest inventoryRequest;

    @Column(name = "product_id")
    private Long productId; // Can be null for manual products

    @Column(name = "product_name")
    private String productName; // Required for manual products

    @Column(name = "job_id")
    private Long jobId;

    @Column(name = "cost_center_id")
    private Long costCenterId;

    @Column(name = "product_code")
    private String productCode; // Null for manual products

    @Column(name = "is_inventory")
    private Boolean isInventory = true; // false for manual products

    @Column(name = "requested_quantity")
    private Integer requestedQuantity;


    @Column(name = "unit_cost")
    private Double unitCost;

    @Column(name = "total_cost")
    private Double totalCost;

    @Column(name = "estimated_usage", length = 500)
    private String estimatedUsage;

    @Column(name = "approved_quantity")
    private Integer approvedQuantity;

    @Column(name = "issued_quantity")
    private Integer issuedQuantity;

    @Column(name = "current_stock")
    private Integer currentStock; // Only for inventory items

    @Column(name = "status")
    private String status = "PENDING";

    @Column(name = "notes", length = 1000)
    private String notes;
}