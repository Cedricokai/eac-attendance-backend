package com.example.eac_backend_project.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendMessageRequest {
    private Long recipientId;
    private String content;
    private String attachmentUrl;
    private String attachmentName;
    private String attachmentType;
}
