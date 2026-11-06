package com.example.eac_backend_project.communication;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="communication_backups")
public class CommunicationBackup {
 @Id @Column(length=36) public String id;
 public Instant createdAt=Instant.now();
 public Instant expiresAt;
 public Long actorId;
 @Column(length=30) public String status;
 @Column(length=64) public String checksum;
 public long sizeBytes;
 @Column(length=500) public String error;
}
