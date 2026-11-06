package com.example.eac_backend_project.files;

import com.example.eac_backend_project.entity.inventory.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity @Table(name="file_versions",uniqueConstraints=@UniqueConstraint(columnNames={"item_id","version_number"}))
@Getter @Setter
public class FileVersion {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="item_id") private FileItem item;
    @Column(name="version_number",nullable=false) private long number;
    @Column(nullable=false,length=80) private String storageKey;
    @Column(nullable=false,length=64) private String checksum;
    @Column(nullable=false) private long sizeBytes;
    @Column(nullable=false,length=160) private String mediaType;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) private User editor;
    @Column(nullable=false) private Instant createdAt=Instant.now();
    @Column(length=120) private String reason;
    @Column(length=300) private String actorLabel;
}
