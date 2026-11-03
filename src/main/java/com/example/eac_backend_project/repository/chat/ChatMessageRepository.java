package com.example.eac_backend_project.repository.chat;

import com.example.eac_backend_project.entity.chat.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("SELECT m FROM ChatMessage m WHERE (m.sender.id = :user1Id AND m.recipient.id = :user2Id) " +
           "OR (m.sender.id = :user2Id AND m.recipient.id = :user1Id) ORDER BY m.createdAt ASC")
    List<ChatMessage> findConversationHistory(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);

    @Query("SELECT m FROM ChatMessage m WHERE ((m.sender.id = :user1Id AND m.recipient.id = :user2Id) " +
           "OR (m.sender.id = :user2Id AND m.recipient.id = :user1Id)) " +
           "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY m.createdAt ASC")
    List<ChatMessage> searchConversationHistory(@Param("user1Id") Long user1Id,
                                                @Param("user2Id") Long user2Id,
                                                @Param("keyword") String keyword);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.recipient.id = :recipientId AND m.sender.id = :senderId AND m.isRead = false")
    long countUnreadFromSender(@Param("recipientId") Long recipientId, @Param("senderId") Long senderId);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.recipient.id = :recipientId AND m.isRead = false")
    long countTotalUnreadForUser(@Param("recipientId") Long recipientId);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead = true WHERE m.recipient.id = :recipientId AND m.sender.id = :senderId AND m.isRead = false")
    int markConversationAsRead(@Param("recipientId") Long recipientId, @Param("senderId") Long senderId);

    @Query("SELECT m FROM ChatMessage m WHERE m.sender.id = :userId OR m.recipient.id = :userId ORDER BY m.createdAt DESC")
    List<ChatMessage> findAllByUser(@Param("userId") Long userId);

    @Query("SELECT m FROM ChatMessage m ORDER BY m.createdAt DESC")
    List<ChatMessage> findAllRecentMessages(Pageable pageable);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE (m.sender.id = :user1Id AND m.recipient.id = :user2Id) OR (m.sender.id = :user2Id AND m.recipient.id = :user1Id)")
    long countMessagesBetweenUsers(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);
}
