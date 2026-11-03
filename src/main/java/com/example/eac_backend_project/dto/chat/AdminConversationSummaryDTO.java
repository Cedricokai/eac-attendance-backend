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
public class AdminConversationSummaryDTO {
    private Long user1Id;
    private String user1Name;
    private String user1UserName;
    private String user1Role;
    private String user1JobPosition;

    private Long user2Id;
    private String user2Name;
    private String user2UserName;
    private String user2Role;
    private String user2JobPosition;

    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private long totalMessages;
    private long unreadCount;
}
