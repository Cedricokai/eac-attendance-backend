package com.example.eac_backend_project.files;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
@Entity @Table(name="file_collaboration_snapshots") @Getter @Setter
public class FileCollaborationSnapshot {
    @Id private Long itemId;
    @OneToOne(fetch=FetchType.LAZY,optional=false) @MapsId @JoinColumn(name="item_id") private FileItem item;
    private String epoch;
    private long fileVersion;
    @Lob @Column(columnDefinition="LONGBLOB") private byte[] state;
    private Instant updatedAt=Instant.now();
}
