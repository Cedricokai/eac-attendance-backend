package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.entity.attendance.ActivityLog;
import com.example.eac_backend_project.repository.ActivityLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityLogService {

    private static final Logger logger = LoggerFactory.getLogger(ActivityLogService.class);

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Transactional
    public void log(String message, String type, String source, String username, Long relatedEntityId) {
        ActivityLog log = new ActivityLog();
        log.setMessage(message);
        log.setType(type);
        log.setSource(source);
        log.setUsername(username);
        log.setRelatedEntityId(relatedEntityId);
        activityLogRepository.save(log);
        // Also write to standard log
        logger.info("ACTIVITY LOG [{}][{}] {} (by {})", source, type, message, username);
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : "SYSTEM";
    }

    public void logInfo(String message, String source, String username, Long relatedEntityId) {
        log(message, "INFO", source, username, relatedEntityId);
    }

    public void logSuccess(String message, String source, String username, Long relatedEntityId) {
        log(message, "SUCCESS", source, username, relatedEntityId);
    }

    public void logWarning(String message, String source, String username, Long relatedEntityId) {
        log(message, "WARNING", source, username, relatedEntityId);
    }

    public void logError(String message, String source, String username, Long relatedEntityId) {
        log(message, "ERROR", source, username, relatedEntityId);
    }
}