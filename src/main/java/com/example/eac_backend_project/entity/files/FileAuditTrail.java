package com.example.eac_backend_project.entity.files;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "file_audit_trails")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileAuditTrail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long fileId;

    @Column(nullable = false, length = 50)
    private String action; // UPLOAD, VIEW, PREVIEW, EDIT, DOWNLOAD, SHARE, VERSION_CREATE

    @Column(nullable = false)
    private String performedBy;

    private String performedByFullName;

    @Column(length = 1000)
    private String details;

    @CreationTimestamp
    private LocalDateTime performedAt;
}
