package com.example.eac_backend_project.entity.inventory;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity @Data @Table(name="inventory_purchase_lines", indexes={@Index(name="idx_ipl_request",columnList="inventoryRequestId"),@Index(name="idx_ipl_product",columnList="productId")})
public class InventoryPurchaseLine {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 private Long inventoryRequestId;
 private Long requestItemId;
 private Long productId;
 private String route;
 private Long financeRequestId;
 private Long financeItemId;
 private String financeReference;
 private int quantity;
 @Column(precision=19,scale=2) private BigDecimal unitCost;
 private int receivedQuantity;
 private int issuedQuantity;
 private String createdBy;
 private LocalDateTime createdAt=LocalDateTime.now();
}
