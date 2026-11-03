package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.Service.ChatService;
import com.example.eac_backend_project.dto.chat.ChatMessageDTO;
import com.example.eac_backend_project.dto.chat.SendMessageRequest;
import com.example.eac_backend_project.entity.inventory.User;
import com.example.eac_backend_project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final ChatService chatService;
    private final UserRepository userRepository;

    @MessageMapping("/chat.send")
    public ChatMessageDTO handleIncomingChatMessage(@Payload SendMessageRequest request, Principal principal) {
        if (principal == null || principal.getName() == null) {
            log.warn("Unauthorized WebSocket message attempt");
            return null;
        }

        User sender = userRepository.findByUserName(principal.getName()).orElse(null);
        if (sender == null) {
            log.warn("Sender user not found for username: {}", principal.getName());
            return null;
        }

        return chatService.sendMessage(sender, request);
    }
}
