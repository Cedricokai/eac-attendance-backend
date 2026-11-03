package com.example.eac_backend_project.entity.inventory;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_issued_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeIssuedItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long employeeId;
    private String employeeName;
    private String employeeNumber;

    @Column(nullable = false)
    private String category; // PPE, TOOL, UNIFORM, EQUIPMENT, OTHER

    @Column(nullable = false)
    private String itemName;

    private String ppeType;

    @Builder.Default
    private Integer quantity = 1;

    private String serialNumber;
    private String receiptReference;

    private String issueDate; // YYYY-MM-DD
    private String replacementDueDate; // YYYY-MM-DD

    @Builder.Default
    private String status = "ISSUED"; // ISSUED, RETURNED, REPLACED, DAMAGED, LOST

    @Column(length = 2000)
    private String notes;

    private String issuedBy;
    private String acknowledgement;
    private String acknowledgementDate;

    private Long replacesId;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "ISSUED";
        }
        if (this.quantity == null) {
            this.quantity = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

