package com.example.eac_backend_project.entity.directpurchase;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "direct_purchases")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DirectPurchase {
    private Long inventoryRequestId;
    private String inventoryRequestNumber;
    private Long createdByUserId;
    private String createdByName;


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String requestNumber;

    @Column(nullable = false)
    private LocalDate requestDate;

    @Column(nullable = false)
    private String requester;

    @Column(name = "requester_user_id")
    private Long requesterUserId;

    @Column(name = "requester_email")
    private String requesterEmail;

    @Column(nullable = false)
    private String accountant;

    @Column(name = "accountant_user_id")
    private Long accountantUserId;

    @Column(name = "accountant_email")
    private String accountantEmail;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DirectPurchaseStatus status = DirectPurchaseStatus.PENDING;

    @Column(nullable = false)
    private Double totalAmount = 0.0;

    @OneToMany(
            mappedBy = "directPurchase",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<DirectPurchaseItem> items = new ArrayList<>();

    public void addItem(DirectPurchaseItem item) {
        if (item == null) return;
        items.add(item);
        item.setDirectPurchase(this);
        recalculateTotal();
    }

    public void removeItem(DirectPurchaseItem item) {
        if (item == null) return;
        items.remove(item);
        item.setDirectPurchase(null);
        recalculateTotal();
    }

    public void recalculateTotal() {
        this.totalAmount = items == null
                ? 0.0
                : items.stream()
                .mapToDouble(item -> item.getTotal() == null ? 0.0 : item.getTotal())
                .sum();
    }
}
