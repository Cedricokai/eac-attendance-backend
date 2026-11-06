package com.example.eac_backend_project.communication;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="communication_uploads",uniqueConstraints=@UniqueConstraint(columnNames="file_id"))
public class CommunicationUpload {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(name="file_id") public Long fileId;
 public Long conversationId;
 public Long ownerId;
 @Column(length=80) public String context;
 @Lob @Column(columnDefinition="LONGBLOB") public byte[] metadata;
 public Instant createdAt=Instant.now();
}
