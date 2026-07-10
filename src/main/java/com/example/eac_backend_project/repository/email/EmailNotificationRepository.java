package com.example.eac_backend_project.repository.email;


import com.example.eac_backend_project.entity.EmailNotification;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface
EmailNotificationRepository extends JpaRepository<EmailNotification, Long> {

    List<EmailNotification> findByRecipientEmailOrderByCreatedAtDesc(String email);

    List<EmailNotification> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);

    Page<EmailNotification> findByRecipientEmail(String email, Pageable pageable);

    List<EmailNotification> findByStatus(String status);

    List<EmailNotification> findByReadFalseAndRecipientEmail(String email);

    long countByRecipientEmailAndReadFalse(String email);

    @Query("SELECT e FROM EmailNotification e WHERE e.createdAt >= :startDate AND e.createdAt <= :endDate")
    List<EmailNotification> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

    List<EmailNotification> findByLeave_Id(Long leaveId);

    // In EmailNotificationRepository.java
    void deleteByLeaveId(Long leaveId);

    // Optional: Also add this for finding
    List<EmailNotification> findByLeaveId(Long leaveId);



    @Modifying
    @Transactional
    @Query("UPDATE EmailNotification n SET n.read = true WHERE n.recipientEmail = :email")
    void markAllAsReadByRecipientEmail(String email);


}