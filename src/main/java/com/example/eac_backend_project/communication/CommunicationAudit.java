package com.example.eac_backend_project.communication;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="communication_audit",indexes=@Index(name="idx_comm_audit",columnList="created_at,conversation_id"))
public class CommunicationAudit {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public Long actorId;
 @Column(length=200) public String actor;
 @Column(length=60) public String action;
 @Column(name="conversation_id") public Long conversationId;
 @Column(length=500) public String reason;
 @Lob public String details;
 @Column(name="created_at") public Instant createdAt=Instant.now();
}
