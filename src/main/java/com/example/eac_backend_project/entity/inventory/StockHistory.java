package com.example.eac_backend_project.entity.inventory;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "stock_history")
public class StockHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "old_stock")
    private Integer oldStock;

    @Column(name = "new_stock")
    private Integer newStock;

    @Column(name = "quantity_changed")
    private Integer quantityChanged;

    @Column(name = "adjustment_type")
    private String adjustmentType; // "ADD" or "REMOVE"

    @Column(name = "reason")
    private String reason;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "user_email")
    private String userEmail;

    @Column(name = "adjustment_date")
    private LocalDateTime adjustmentDate;

    @PrePersist
    protected void onCreate() {
        if (adjustmentDate == null) {
            adjustmentDate = LocalDateTime.now();
        }
        if (quantityChanged == null && oldStock != null && newStock != null) {
            quantityChanged = Math.abs(newStock - oldStock);
        }
    }

    // Getters and setters are handled by @Data annotation
}