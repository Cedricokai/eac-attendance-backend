package com.example.eac_backend_project.entity.directpurchase;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "direct_purchase_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DirectPurchaseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(nullable = false)
    private Double unitPrice = 0.0;

    @Column(nullable = false)
    private Double total = 0.0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "direct_purchase_id", nullable = false)
    private DirectPurchase directPurchase;

    // Automatically calculate total before persist/update
    @PrePersist
    @PreUpdate
    public void calculateTotal() {
        if (quantity != null && unitPrice != null) {
            this.total = quantity * unitPrice;
        }
    }
}