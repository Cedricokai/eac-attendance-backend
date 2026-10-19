package com.example.eac_backend_project.entity.finance;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.math.BigDecimal;
@Entity @Table(name="petty_cash_items") @Data
public class PettyCashItem {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="request_id") @JsonIgnore @ToString.Exclude @EqualsAndHashCode.Exclude private PettyCashRequest request;
 @Column(nullable=false,length=300) private String description;
 @Column(nullable=false,precision=12,scale=2) private BigDecimal quantity;
 @Column(name="estimated_unit_cost",nullable=false,precision=19,scale=2) private BigDecimal estimatedUnitCost;
 @Column(name="estimated_total",nullable=false,precision=19,scale=2) private BigDecimal estimatedTotal;
 @Column(name="actual_unit_cost",precision=19,scale=2) private BigDecimal actualUnitCost;
 @Column(name="actual_quantity",precision=12,scale=2) private BigDecimal actualQuantity;
 @Column(name="actual_total",precision=19,scale=2) private BigDecimal actualTotal;
}
