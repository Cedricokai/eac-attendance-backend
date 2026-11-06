package com.example.eac_backend_project.files;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
@Component public class FileRetentionScheduler {
 private final FileRetentionService retention;
 public FileRetentionScheduler(FileRetentionService retention){this.retention=retention;}
 @Scheduled(cron="${app.files.retention-cron:0 30 3 * * *}",zone="UTC")public void run(){try{retention.run();}catch(Exception ex){org.slf4j.LoggerFactory.getLogger(getClass()).error("File retention failed ({})",ex.getClass().getSimpleName());}}
}
