package com.example.eac_backend_project.files;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
@Entity @Table(name="file_backup_policy") @Getter @Setter
public class FileBackupPolicy {
 @Id private Long id=1L;
 private boolean enabled=false;
 private String frequency="DAILY";
 private String destination="FILESYSTEM";
 private int retainCount=14;
 private long maxBackupBytes=1073741824L;
 private Instant lastRun;
 private Instant nextRun;
 private String lastStatus;
 @Version private long revision;
}
