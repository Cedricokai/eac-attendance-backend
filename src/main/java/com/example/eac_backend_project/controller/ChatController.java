package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.Service.ChatService;
import com.example.eac_backend_project.dto.chat.ChatContactDTO;
import com.example.eac_backend_project.dto.chat.ChatMessageDTO;
import com.example.eac_backend_project.dto.chat.SendMessageRequest;
import com.example.eac_backend_project.entity.inventory.User;
import com.example.eac_backend_project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final UserRepository userRepository;

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("Unauthorized: User not authenticated");
        }
        return userRepository.findByUserName(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in database: " + authentication.getName()));
    }

    @GetMapping("/contacts")
    public ResponseEntity<List<ChatContactDTO>> getContacts(Authentication authentication) {
        User currentUser = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(chatService.getContacts(currentUser));
    }

    @GetMapping("/history/{contactId}")
    public ResponseEntity<List<ChatMessageDTO>> getHistory(@PathVariable Long contactId, Authentication authentication) {
        User currentUser = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(chatService.getConversationHistory(currentUser, contactId));
    }

    @PostMapping("/send")
    public ResponseEntity<ChatMessageDTO> sendMessage(@RequestBody SendMessageRequest request, Authentication authentication) {
        User currentUser = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(chatService.sendMessage(currentUser, request));
    }

    @PostMapping("/mark-read/{contactId}")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable Long contactId, Authentication authentication) {
        User currentUser = getAuthenticatedUser(authentication);
        int updated = chatService.markAsRead(currentUser, contactId);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("updatedCount", updated);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(Authentication authentication) {
        User currentUser = getAuthenticatedUser(authentication);
        long totalUnread = chatService.getTotalUnreadCount(currentUser);
        Map<String, Object> res = new HashMap<>();
        res.put("unreadCount", totalUnread);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadAttachment(@RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(chatService.saveAttachment(file));
        } catch (Exception e) {
            log.error("Failed to upload chat attachment", e);
            Map<String, String> err = new HashMap<>();
            err.put("error", "Failed to upload file: " + e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @GetMapping("/attachments/{fileName:.+}")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable String fileName) {
        try {
            Path filePath = Paths.get("uploads/chat-attachments").resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                String contentType = "application/octet-stream";
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException ex) {
            return ResponseEntity.badRequest().build();
        }
    }
}
