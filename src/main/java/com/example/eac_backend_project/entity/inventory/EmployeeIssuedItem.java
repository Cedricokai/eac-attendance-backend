package com.example.eac_backend_project.entity.inventory;
import jakarta.persistence.*;
import lombok.Data;
import java.time.*;

@Entity @Data @Table(name = "employee_issued_item")
public class EmployeeIssuedItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Version private Long version;
    @Column(nullable = false) private Long employeeId;
    private String employeeName;
    private String employeeNumber;
    @Column(nullable = false) private String itemName;
    private String category;
    private String ppeType;
    private Integer quantity;
    private String serialNumber;
    private String receiptReference;
    @Column(nullable = false) private LocalDate issueDate;
    private LocalDate replacementDueDate;
    private Integer replacementIntervalCount;
    private String replacementIntervalUnit;
    private String issuedBy;
    private String recordedBy;
    private Instant recordedAt;
    private String status;
    private LocalDate statusDate;
    @Column(length = 2000) private String notes;
    private String acknowledgement;
    private LocalDate acknowledgementDate;
    private Long replacesId;
    private Long replacedById;
}
