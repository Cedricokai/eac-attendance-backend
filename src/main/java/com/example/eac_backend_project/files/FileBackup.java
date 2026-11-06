package com.example.eac_backend_project.files;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
@Entity @Table(name="file_backups") @Getter @Setter
public class FileBackup {
 @Id private String id;
 private Instant createdAt=Instant.now();
 private String createdBy;
 private String destination;
 private String status="CREATING";
 private long sizeBytes;
 private int itemCount;
 private int versionCount;
 private String checksum;
 private Instant verifiedAt;
 @Column(length=500) private String message;
}
