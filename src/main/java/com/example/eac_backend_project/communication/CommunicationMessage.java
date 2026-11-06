package com.example.eac_backend_project.communication;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="communication_messages",uniqueConstraints={@UniqueConstraint(columnNames={"conversation_id","sequence_number"}),@UniqueConstraint(columnNames={"conversation_id","sender_id","client_id"})},indexes=@Index(name="idx_comm_messages",columnList="conversation_id,sequence_number"))
public class CommunicationMessage {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="conversation_id") public CommunicationConversation conversation;
 @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="sender_id") public com.example.eac_backend_project.entity.inventory.User sender;
 @Column(name="client_id",nullable=false,length=80) public String clientId;
 @Column(name="sequence_number",nullable=false) public long sequence;
 @Lob @Column(nullable=false,columnDefinition="LONGBLOB") public byte[] ciphertext;
 public Long replyTo;
 public Long forwardedFrom;
 public boolean pinned;
 @Lob public String reactions="{}";
 public Instant createdAt=Instant.now();
 public Instant editedAt;
 public Instant deletedAt;
 public Long deletedBy;
 public Instant retainUntil;
 @Column(length=500) public String deletionReason;
}
