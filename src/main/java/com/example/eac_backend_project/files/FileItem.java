package com.example.eac_backend_project.files;

import com.example.eac_backend_project.entity.inventory.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity @Table(name="file_items", indexes={@Index(name="idx_file_parent",columnList="parent_id"), @Index(name="idx_file_owner",columnList="owner_id")})
@Getter @Setter
public class FileItem {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="owner_id") private User owner;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="parent_id") private FileItem parent;
    @Column(nullable=false,length=180) private String name;
    @Column(nullable=false) private boolean folder;
    @Column(nullable=false,length=160) private String mediaType="application/octet-stream";
    @Column(nullable=false) private long sizeBytes;
    @Column(nullable=false) private long currentVersion;
    @Column(nullable=false) private Instant createdAt=Instant.now();
    @Column(nullable=false) private Instant updatedAt=Instant.now();
    private Instant deletedAt;
    private Long deletionGroup;
    private String deletedBy;
    private String collaborationEpoch=java.util.UUID.randomUUID().toString();
    private String collaborationRoom;
    private Instant collaborationLeaseUntil;
    @Version private long revision;
}
