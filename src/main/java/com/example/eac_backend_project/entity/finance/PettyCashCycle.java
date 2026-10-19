package com.example.eac_backend_project.entity.finance;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "petty_cash_cycles", indexes = {
    @Index(name = "idx_petty_cash_cycle_status", columnList = "status"),
    @Index(name = "idx_petty_cash_cycle_opened", columnList = "opened_at")
})
@Data
public class PettyCashCycle {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "cycle_number", nullable = false, unique = true, length = 40) private String cycleNumber;
    @Column(name = "allocated_amount", nullable = false, precision = 19, scale = 2) private BigDecimal allocatedAmount;
    @Column(nullable = false, length = 20) private String status = "ACTIVE";
    @Column(name = "opened_at", nullable = false) private LocalDateTime openedAt = LocalDateTime.now();
    @Column(name = "opened_by", length = 150) private String openedBy;
    @Column(name = "closed_at") private LocalDateTime closedAt;
    @Column(name = "closed_by", length = 150) private String closedBy;
    @Column(name = "reimbursement_amount", precision = 19, scale = 2) private BigDecimal reimbursementAmount;
    @Column(name = "reimbursement_reference", length = 150) private String reimbursementReference;
    @Column(name = "reimbursement_notes", columnDefinition = "TEXT") private String reimbursementNotes;
}
