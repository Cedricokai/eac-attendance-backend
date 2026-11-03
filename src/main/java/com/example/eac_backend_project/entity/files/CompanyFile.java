package com.example.eac_backend_project.entity.files;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "company_files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String storedFileName;

    @Column(nullable = false)
    private String filePath;

    private String fileType;

    @Column(length = 20)
    private String extension;

    private Long fileSize;

    @Column(nullable = false)
    private String uploadedBy;

    private String uploadedByFullName;

    @Builder.Default
    private Integer version = 1;

    @Builder.Default
    private Boolean isEdited = false;

    // References the original root file if this is an edited revision
    private Long originalFileId;

    @Column(length = 1000)
    private String description;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Builder.Default
    private Boolean isDeleted = false;

    private String deletedBy;

    private LocalDateTime deletedAt;

    private String lastModifiedBy;
}
