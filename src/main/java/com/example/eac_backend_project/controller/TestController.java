package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.Service.email.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
public class TestController {

    @Autowired
    private EmailService emailService;

//    @PostMapping("/send-test-email")
//    public ResponseEntity<String> sendTestEmail(@RequestParam String to) {
//        try {
//            emailService.sendCustomEmail(to, "Test Email", "This is a test email from the system.");
//            return ResponseEntity.ok("Test email sent to: " + to);
//        } catch (Exception e) {
//            return ResponseEntity.status(500).body("Failed to send email: " + e.getMessage());
//        }
//    }


    @GetMapping("/test-email")
    public String testEmail() {
        try {
            String testEmail = "okaicedric4@gmail.com"; // Send to yourself first
            String subject = "Test Email from EAC System";
            String content = "<h1>Test Email</h1><p>If you receive this, email is working!</p>";

            emailService.sendCustomEmail(testEmail, subject, content);
            return "Test email sent to: " + testEmail;

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

//    @GetMapping("/status")
//    public ResponseEntity<?> getStatus() {
//        Map<String, Object> response = new HashMap<>();
//        response.put("status", "Backend is running");
//        response.put("timestamp", LocalDateTime.now().toString());
//        response.put("cors", "CORS is configured");
//        return ResponseEntity.ok(response);
//    }
//
//    @PostMapping("/echo")
//    public ResponseEntity<?> echo(@RequestBody Map<String, Object> data) {
//        Map<String, Object> response = new HashMap<>();
//        response.put("received", data);
//        response.put("timestamp", LocalDateTime.now().toString());
//        return ResponseEntity.ok(response);
//    }
}