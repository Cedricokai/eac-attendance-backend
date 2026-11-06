package com.example.eac_backend_project.communication;
import jakarta.persistence.*;
import java.time.Instant;
@org.hibernate.annotations.Proxy(lazy=false)
@Entity @Table(name="communication_conversations",uniqueConstraints=@UniqueConstraint(columnNames="direct_key"))
public class CommunicationConversation {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(name="direct_key",length=90) public String directKey;
 @Column(nullable=false,length=24) public String type="DIRECT";
 @Column(nullable=false,length=140) public String name="";
 @Column(length=1000) public String description="";
 @Column(length=24) public String groupType="STANDARD";
 public boolean announcements;
 @Column(name="last_sequence") public long sequence;
 public Long creatorId;
 public Instant createdAt=Instant.now();
 public Instant updatedAt=Instant.now();
 public Instant deletedAt;
 public Instant retainUntil;
 public Long deletedBy;
 @Version public long revision;
}
