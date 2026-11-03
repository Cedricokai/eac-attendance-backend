package com.example.eac_backend_project.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatContactDTO {
    private Long id;
    private String name;
    private String userName;
    private String email;
    private String role;
    private String department;
    private String jobPosition;
    private long unreadCount;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private boolean isOnline;
}
