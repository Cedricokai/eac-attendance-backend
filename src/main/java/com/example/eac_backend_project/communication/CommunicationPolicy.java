package com.example.eac_backend_project.communication;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="communication_policy")
public class CommunicationPolicy {
 @Id public Long id=1L;
 public boolean enabled=true;
 public boolean groupsEnabled=true;
 public boolean filesEnabled=true;
 public boolean voiceEnabled=true;
 public boolean editingEnabled=true;
 public boolean deletionEnabled=false;
 public boolean conversationDeletionEnabled=false;
 public boolean groupModeratorDeletion=false;
 public boolean groupCreationEnabled=true;
 public boolean membershipManagementEnabled=true;
 public boolean presenceEnabled=true;
 public boolean receiptsEnabled=true;
 public boolean backupsEnabled=true;
 public int editMinutes=15;
 public int deleteMinutes=15;
 public int retentionDays=90;
 public int maxVoiceSeconds=180;
 public long maxAttachmentBytes=10485760;
 public int backupRetentionDays=30;
 public int backupIntervalHours=24;
 public Long updatedBy;
 public Instant updatedAt=Instant.now();
}
