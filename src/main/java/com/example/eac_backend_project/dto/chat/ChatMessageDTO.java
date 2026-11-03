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
public class ChatMessageDTO {
    private Long id;
    private Long senderId;
    private String senderName;
    private String senderUserName;
    private String senderRole;
    private Long recipientId;
    private String recipientName;
    private String recipientUserName;
    private String recipientRole;
    private String content;
    private boolean isRead;
    private LocalDateTime createdAt;
    private String attachmentUrl;
    private String attachmentName;
    private String attachmentType;
}
