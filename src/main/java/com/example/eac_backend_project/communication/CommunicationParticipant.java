package com.example.eac_backend_project.communication;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="communication_participants",uniqueConstraints=@UniqueConstraint(columnNames={"conversation_id","user_id"}),indexes=@Index(name="idx_comm_user",columnList="user_id,left_at"))
public class CommunicationParticipant {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="conversation_id") public CommunicationConversation conversation;
 @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="user_id") public com.example.eac_backend_project.entity.inventory.User user;
 @Column(name="left_at") public Instant leftAt;
 public Instant joinedAt=Instant.now();
 public long joinedSequence=1;
 public long lastRead;
 public long lastDelivered;
 public boolean manager;
 public boolean muted;
 public boolean pinned;
 public boolean archived;
}
