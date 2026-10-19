package com.example.eac_backend_project.entity.inventory;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
@Entity @Data @Table(name="inventory_procurement_proposals")
public class InventoryProcurementProposal {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private Long inventoryRequestId;
 @Column(nullable=false,unique=true) private String reference;
 @Column(nullable=false) private String supplier;
 @Column(nullable=false,length=500) private String quotationReference;
 @Column(length=800) private String notes;
 @Column(nullable=false,precision=19,scale=2) private BigDecimal totalAmount;
 @Column(nullable=false) private String status="AWAITING_ACCOUNTS";
 private Long submittedByUserId;
 private String submittedBy;
 private LocalDateTime submittedAt=LocalDateTime.now();
 private Long reviewedByUserId;
 private String reviewedBy;
 private LocalDateTime reviewedAt;
 @Column(length=800) private String reviewNotes;
 private String fundingRoute;
 private Long financeRequestId;
 private String financeReference;
 @OneToMany(mappedBy="proposal",cascade=CascadeType.ALL,orphanRemoval=true,fetch=FetchType.EAGER) @OrderBy("id ASC")
 private List<InventoryProposalItem> items=new ArrayList<>();
}
