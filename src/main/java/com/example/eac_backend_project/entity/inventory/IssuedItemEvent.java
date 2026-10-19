package com.example.eac_backend_project.entity.inventory;
import jakarta.persistence.*;
import lombok.Data;
import java.time.*;

@Entity @Data @Table(name = "issued_item_event")
public class IssuedItemEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long itemId;
    private String action;
    private String recordedBy;
    private Instant recordedAt;
    private LocalDate effectiveDate;
    @Column(length = 2000) private String notes;
}
