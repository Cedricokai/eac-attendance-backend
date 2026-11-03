package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.dto.chat.AdminConversationSummaryDTO;
import com.example.eac_backend_project.dto.chat.ChatContactDTO;
import com.example.eac_backend_project.dto.chat.ChatMessageDTO;
import com.example.eac_backend_project.dto.chat.SendMessageRequest;
import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.chat.ChatMessage;
import com.example.eac_backend_project.entity.inventory.User;
import com.example.eac_backend_project.repository.UserRepository;
import com.example.eac_backend_project.repository.chat.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String UPLOAD_DIR = "uploads/chat-attachments";

    private Path getStoragePath() {
        Path path = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            log.error("Failed to create chat attachments directory", e);
        }
        return path;
    }

    /**
     * Map ChatMessage entity to DTO
     */
    public ChatMessageDTO mapToDTO(ChatMessage message) {
        User sender = message.getSender();
        User recipient = message.getRecipient();

        return ChatMessageDTO.builder()
                .id(message.getId())
                .senderId(sender != null ? sender.getId() : null)
                .senderName(sender != null ? sender.getName() : "Unknown")
                .senderUserName(sender != null ? sender.getUserName() : "unknown")
                .senderRole(sender != null && sender.getRole() != null ? sender.getRole().getName() : "")
                .recipientId(recipient != null ? recipient.getId() : null)
                .recipientName(recipient != null ? recipient.getName() : "Unknown")
                .recipientUserName(recipient != null ? recipient.getUserName() : "unknown")
                .recipientRole(recipient != null && recipient.getRole() != null ? recipient.getRole().getName() : "")
                .content(message.getContent())
                .isRead(message.isRead())
                .createdAt(message.getCreatedAt())
                .attachmentUrl(message.getAttachmentUrl())
                .attachmentName(message.getAttachmentName())
                .attachmentType(message.getAttachmentType())
                .build();
    }

    /**
     * Send message between employees and broadcast via WebSockets
     */
    @Transactional
    public ChatMessageDTO sendMessage(User sender, SendMessageRequest request) {
        if (request.getRecipientId() == null) {
            throw new IllegalArgumentException("Recipient ID must not be null");
        }

        if (sender.getId().equals(request.getRecipientId())) {
            throw new IllegalArgumentException("Cannot send message to yourself");
        }

        User recipient = userRepository.findById(request.getRecipientId())
                .orElseThrow(() -> new IllegalArgumentException("Recipient user not found: " + request.getRecipientId()));

        ChatMessage chatMessage = ChatMessage.builder()
                .sender(sender)
                .recipient(recipient)
                .content(request.getContent() != null ? request.getContent().trim() : "")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .attachmentUrl(request.getAttachmentUrl())
                .attachmentName(request.getAttachmentName())
                .attachmentType(request.getAttachmentType())
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);
        ChatMessageDTO dto = mapToDTO(savedMessage);

        try {
            // Send to recipient's private user queue
            messagingTemplate.convertAndSendToUser(
                    recipient.getUserName(),
                    "/queue/messages",
                    dto
            );

            // Send copy back to sender's private queue
            messagingTemplate.convertAndSendToUser(
                    sender.getUserName(),
                    "/queue/messages",
                    dto
            );

            // Real-time broadcast to Admin audit topic
            messagingTemplate.convertAndSend("/topic/admin/chat", dto);
        } catch (Exception e) {
            log.warn("Failed to push real-time WebSocket message: {}", e.getMessage());
        }

        return dto;
    }

    /**
     * Get list of all contacts for current user with last message and unread badge
     */
    @Transactional(readOnly = true)
    public List<ChatContactDTO> getContacts(User currentUser) {
        List<User> allUsers = userRepository.findByEnabledTrue();

        // Load all messages for currentUser once to build conversation metadata fast
        List<ChatMessage> userMessages = chatMessageRepository.findAllByUser(currentUser.getId());

        Map<Long, ChatMessage> latestMessageMap = new HashMap<>();
        for (ChatMessage msg : userMessages) {
            Long partnerId = msg.getSender().getId().equals(currentUser.getId())
                    ? msg.getRecipient().getId()
                    : msg.getSender().getId();

            if (!latestMessageMap.containsKey(partnerId)) {
                latestMessageMap.put(partnerId, msg);
            }
        }

        List<ChatContactDTO> contacts = new ArrayList<>();

        for (User user : allUsers) {
            if (user.getId().equals(currentUser.getId())) {
                continue;
            }

            ChatMessage lastMsg = latestMessageMap.get(user.getId());
            long unread = chatMessageRepository.countUnreadFromSender(currentUser.getId(), user.getId());

            String jobPosition = "Staff";
            String department = "";
            if (user.getEmployee() != null) {
                Employee emp = user.getEmployee();
                if (emp.getJobPosition() != null) {
                    jobPosition = emp.getJobPosition();
                }
                if (emp.getCategory() != null && emp.getCategory().getName() != null) {
                    department = emp.getCategory().getName();
                }
            }

            String lastSnippet = "";
            LocalDateTime lastTime = null;
            if (lastMsg != null) {
                if (lastMsg.getContent() != null && !lastMsg.getContent().isBlank()) {
                    lastSnippet = lastMsg.getContent();
                } else if (lastMsg.getAttachmentName() != null) {
                    lastSnippet = "📎 " + lastMsg.getAttachmentName();
                }
                lastTime = lastMsg.getCreatedAt();
            }

            contacts.add(ChatContactDTO.builder()
                    .id(user.getId())
                    .name(user.getName())
                    .userName(user.getUserName())
                    .email(user.getEmail())
                    .role(user.getRole() != null ? user.getRole().getName() : "")
                    .department(department)
                    .jobPosition(jobPosition)
                    .unreadCount(unread)
                    .lastMessage(lastSnippet)
                    .lastMessageTime(lastTime)
                    .isOnline(true)
                    .build());
        }

        // Sort: contacts with latest messages first, then by name
        contacts.sort((a, b) -> {
            if (a.getLastMessageTime() != null && b.getLastMessageTime() != null) {
                return b.getLastMessageTime().compareTo(a.getLastMessageTime());
            } else if (a.getLastMessageTime() != null) {
                return -1;
            } else if (b.getLastMessageTime() != null) {
                return 1;
            }
            return a.getName().compareToIgnoreCase(b.getName());
        });

        return contacts;
    }

    /**
     * Retrieve full conversation history between currentUser and a contact
     */
    @Transactional(readOnly = true)
    public List<ChatMessageDTO> getConversationHistory(User currentUser, Long contactId) {
        List<ChatMessage> messages = chatMessageRepository.findConversationHistory(currentUser.getId(), contactId);
        return messages.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    /**
     * Mark incoming messages from contactId to currentUser as read
     */
    @Transactional
    public int markAsRead(User currentUser, Long contactId) {
        int updated = chatMessageRepository.markConversationAsRead(currentUser.getId(), contactId);
        if (updated > 0) {
            // Notify the sender that messages were marked read
            try {
                User sender = userRepository.findById(contactId).orElse(null);
                if (sender != null) {
                    Map<String, Object> readEvent = new HashMap<>();
                    readEvent.put("type", "READ_RECEIPT");
                    readEvent.put("readerId", currentUser.getId());
                    readEvent.put("readerUserName", currentUser.getUserName());
                    messagingTemplate.convertAndSendToUser(sender.getUserName(), "/queue/messages", readEvent);
                }
            } catch (Exception e) {
                log.warn("Failed to notify read receipt: {}", e.getMessage());
            }
        }
        return updated;
    }

    /**
     * Total unread messages count for header badge
     */
    @Transactional(readOnly = true)
    public long getTotalUnreadCount(User currentUser) {
        return chatMessageRepository.countTotalUnreadForUser(currentUser.getId());
    }

    /**
     * Save an uploaded file attachment for chat
     */
    public Map<String, String> saveAttachment(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        String originalFilename = Paths.get(file.getOriginalFilename()).getFileName().toString();
        String uniqueFileName = UUID.randomUUID().toString() + "_" + originalFilename.replaceAll("\\s+", "_");

        Path targetLocation = getStoragePath().resolve(uniqueFileName);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        String fileUrl = "/api/chat/attachments/" + uniqueFileName;

        Map<String, String> response = new HashMap<>();
        response.put("url", fileUrl);
        response.put("fileName", originalFilename);
        response.put("fileType", file.getContentType());
        return response;
    }

    // =========================================================================
    // ADMIN AUDIT CAPABILITIES
    // =========================================================================

    /**
     * Administrator: retrieve overview of all employee conversation pairs in company
     */
    @Transactional(readOnly = true)
    public List<AdminConversationSummaryDTO> getAllConversationsForAdmin() {
        List<ChatMessage> allMessages = chatMessageRepository.findAll();

        // Group messages by canonical user pair key: "min(id)_max(id)"
        Map<String, List<ChatMessage>> threadMap = new HashMap<>();
        for (ChatMessage msg : allMessages) {
            long id1 = Math.min(msg.getSender().getId(), msg.getRecipient().getId());
            long id2 = Math.max(msg.getSender().getId(), msg.getRecipient().getId());
            String key = id1 + "_" + id2;
            threadMap.computeIfAbsent(key, k -> new ArrayList<>()).add(msg);
        }

        List<AdminConversationSummaryDTO> summaries = new ArrayList<>();

        for (Map.Entry<String, List<ChatMessage>> entry : threadMap.entrySet()) {
            List<ChatMessage> threadMsgs = entry.getValue();
            if (threadMsgs.isEmpty()) continue;

            // Sort to find latest
            threadMsgs.sort(Comparator.comparing(ChatMessage::getCreatedAt).reversed());
            ChatMessage latest = threadMsgs.get(0);

            User user1 = latest.getSender().getId() < latest.getRecipient().getId()
                    ? latest.getSender()
                    : latest.getRecipient();
            User user2 = latest.getSender().getId() < latest.getRecipient().getId()
                    ? latest.getRecipient()
                    : latest.getSender();

            String u1Pos = user1.getEmployee() != null && user1.getEmployee().getJobPosition() != null
                    ? user1.getEmployee().getJobPosition() : "Staff";
            String u2Pos = user2.getEmployee() != null && user2.getEmployee().getJobPosition() != null
                    ? user2.getEmployee().getJobPosition() : "Staff";

            String snippet = latest.getContent() != null && !latest.getContent().isBlank()
                    ? latest.getContent()
                    : (latest.getAttachmentName() != null ? "📎 " + latest.getAttachmentName() : "");

            long unread = threadMsgs.stream().filter(m -> !m.isRead()).count();

            summaries.add(AdminConversationSummaryDTO.builder()
                    .user1Id(user1.getId())
                    .user1Name(user1.getName())
                    .user1UserName(user1.getUserName())
                    .user1Role(user1.getRole() != null ? user1.getRole().getName() : "")
                    .user1JobPosition(u1Pos)
                    .user2Id(user2.getId())
                    .user2Name(user2.getName())
                    .user2UserName(user2.getUserName())
                    .user2Role(user2.getRole() != null ? user2.getRole().getName() : "")
                    .user2JobPosition(u2Pos)
                    .lastMessage(snippet)
                    .lastMessageTime(latest.getCreatedAt())
                    .totalMessages(threadMsgs.size())
                    .unreadCount(unread)
                    .build());
        }

        summaries.sort((a, b) -> {
            if (a.getLastMessageTime() != null && b.getLastMessageTime() != null) {
                return b.getLastMessageTime().compareTo(a.getLastMessageTime());
            }
            return 0;
        });

        return summaries;
    }

    /**
     * Administrator: full message transcript between any two employees
     */
    @Transactional(readOnly = true)
    public List<ChatMessageDTO> getAdminConversationHistory(Long user1Id, Long user2Id, String keyword) {
        List<ChatMessage> list;
        if (keyword != null && !keyword.trim().isEmpty()) {
            list = chatMessageRepository.searchConversationHistory(user1Id, user2Id, keyword.trim());
        } else {
            list = chatMessageRepository.findConversationHistory(user1Id, user2Id);
        }
        return list.stream().map(this::mapToDTO).collect(Collectors.toList());
    }
}
