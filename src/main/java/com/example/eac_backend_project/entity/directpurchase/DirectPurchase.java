package com.example.eac_backend_project.entity.directpurchase;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "direct_purchases")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DirectPurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String requestNumber;

    @Column(nullable = false)
    private LocalDate requestDate;

    @Column(nullable = false)
    private String requester; // procurement officer

    @Column(nullable = false)
    private String accountant; // approving officer

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DirectPurchaseStatus status = DirectPurchaseStatus.PENDING;

    @Column(nullable = false)
    private Double totalAmount = 0.0;

    @OneToMany(mappedBy = "directPurchase", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DirectPurchaseItem> items = new ArrayList<>();

    // Helper method to add item
    public void addItem(DirectPurchaseItem item) {
        items.add(item);
        item.setDirectPurchase(this);
        recalculateTotal();
    }

    // Helper method to remove item
    public void removeItem(DirectPurchaseItem item) {
        items.remove(item);
        item.setDirectPurchase(null);
        recalculateTotal();
    }

    // Recalculate total amount from items
    public void recalculateTotal() {
        this.totalAmount = items.stream()
                .mapToDouble(DirectPurchaseItem::getTotal)
                .sum();
    }
}