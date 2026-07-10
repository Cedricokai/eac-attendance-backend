package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.Service.email.EmailNotificationService;
import com.example.eac_backend_project.entity.EmailNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private EmailNotificationService emailNotificationService;

    // Get all notifications for the authenticated user
    @GetMapping
    public ResponseEntity<List<EmailNotification>> getMyNotifications(Authentication auth) {
        String email = auth.getName(); // assuming email is the principal name
        return ResponseEntity.ok(emailNotificationService.getNotificationsForUser(email));
    }

    // Get unread count
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication auth) {
        String email = auth.getName();
        long count = emailNotificationService.getUnreadCountForUser(email);
        return ResponseEntity.ok(Map.of("count", count));
    }

    // Mark a single notification as read
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id, Authentication auth) {
        String email = auth.getName();
        emailNotificationService.markAsRead(id, email);
        return ResponseEntity.ok().build();
    }

    // Mark all as read
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(Authentication auth) {
        String email = auth.getName();
        emailNotificationService.markAllAsRead(email);
        return ResponseEntity.ok().build();
    }
}