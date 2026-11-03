package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.Service.ChatService;
import com.example.eac_backend_project.dto.chat.AdminConversationSummaryDTO;
import com.example.eac_backend_project.dto.chat.ChatMessageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/chat")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN') or hasRole('ROLE_ADMIN')")
public class AdminChatController {

    private final ChatService chatService;

    /**
     * Get summary of all conversations in the organization
     */
    @GetMapping("/conversations")
    public ResponseEntity<List<AdminConversationSummaryDTO>> getAllConversations() {
        return ResponseEntity.ok(chatService.getAllConversationsForAdmin());
    }

    /**
     * Get complete audit transcript between any two employees
     */
    @GetMapping("/conversation/{user1Id}/{user2Id}")
    public ResponseEntity<List<ChatMessageDTO>> getConversationAudit(
            @PathVariable Long user1Id,
            @PathVariable Long user2Id,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ResponseEntity.ok(chatService.getAdminConversationHistory(user1Id, user2Id, keyword));
    }
}
