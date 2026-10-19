package com.example.eac_backend_project.entity.portal;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "support_requests", indexes = @Index(name = "idx_support_request_status", columnList = "status"))
@Data
public class SupportRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 32) private String reference;
    @Column(nullable = false, length = 150) private String name;
    @Column(nullable = false, length = 180) private String email;
    @Column(nullable = false, length = 40) private String category;
    @Column(nullable = false, columnDefinition = "TEXT") private String description;
    @Column(length = 40) private String page;
    @Column(nullable = false, length = 24) private String status = "OPEN";
    @Column(name = "resolution_notes", columnDefinition = "TEXT") private String resolutionNotes;
    @Column(name = "assigned_to", length = 120) private String assignedTo;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt = LocalDateTime.now();
}
