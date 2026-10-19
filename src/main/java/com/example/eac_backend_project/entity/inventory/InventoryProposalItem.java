package com.example.eac_backend_project.entity.inventory;
import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
@Entity @Data @Table(name="inventory_proposal_items")
public class InventoryProposalItem {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(nullable=false) @JsonIgnore private InventoryProcurementProposal proposal;
 @Column(nullable=false) private Long requestItemId;
 private String productName;
 private int quantity;
 @Column(nullable=false,precision=19,scale=2) private BigDecimal unitCost;
}
