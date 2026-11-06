package com.example.eac_backend_project.files;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
@Component
public class FileBackupScheduler {
 private final FileBackupService backups;private final FileNotificationService notifications;
 public FileBackupScheduler(FileBackupService backups,FileNotificationService notifications){this.backups=backups;this.notifications=notifications;}
 @Scheduled(fixedDelayString="${app.files.backup-poll-ms:60000}",initialDelay=60000)
 public void run(){try{backups.createScheduled();}catch(Exception ex){org.slf4j.LoggerFactory.getLogger(getClass()).error("Scheduled file backup failed ({})",ex.getClass().getSimpleName());backups.recordFailure();notifications.backupFailed();}}
}
