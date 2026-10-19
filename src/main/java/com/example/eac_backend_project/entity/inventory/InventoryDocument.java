package com.example.eac_backend_project.entity.inventory;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_documents", indexes = {
        @Index(name = "idx_inventory_document_uploaded_at", columnList = "uploaded_at"),
        @Index(name = "idx_inventory_document_location", columnList = "document_location")
})
@Getter @Setter
public class InventoryDocument {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_name", nullable = false, length = 200)
    private String name;

    @Column(name = "document_location", nullable = false, length = 250)
    private String location;

    @Column(name = "document_type", nullable = false, length = 50)
    private String type;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 150)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long size;

    @Column(name = "uploaded_by", length = 150)
    private String uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    @Lob
    @Column(name = "file_data", nullable = false, columnDefinition = "MEDIUMBLOB")
    private byte[] fileData;
}
