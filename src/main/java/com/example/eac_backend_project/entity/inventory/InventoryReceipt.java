package com.example.eac_backend_project.entity.inventory;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
@Entity @Data @Table(name="inventory_finance_receipts")
public class InventoryReceipt {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 private Long purchaseLineId;
 private int quantity;
 @Column(length=500) private String evidenceReference;
 private String receivedBy;
 private LocalDateTime receivedAt=LocalDateTime.now();
}
