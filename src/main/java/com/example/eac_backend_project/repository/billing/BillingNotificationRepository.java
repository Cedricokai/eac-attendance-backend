package com.example.eac_backend_project.repository.billing;

import com.example.eac_backend_project.entity.billing.BillingNotification;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface BillingNotificationRepository extends JpaRepository<BillingNotification, Long> {

    @Query("SELECT bn FROM BillingNotification bn WHERE bn.status = 'PENDING' " +
            "AND bn.scheduledDate <= :currentDate " +
            "AND (bn.scheduledTime IS NULL OR bn.scheduledTime <= :currentTime)")
    List<BillingNotification> findPendingNotificationsScheduledForNow(
            @Param("currentDate") LocalDate currentDate,
            @Param("currentTime") LocalTime currentTime
    );

    @Query("SELECT bn FROM BillingNotification bn WHERE bn.status = 'PENDING' " +
            "AND bn.scheduledDate <= :date")
    List<BillingNotification> findPendingNotificationsByDate(@Param("date") LocalDate date);

    @Modifying
    @Transactional
    @Query("UPDATE BillingNotification bn SET bn.status = 'CANCELLED' " +
            "WHERE bn.jobId = :jobId AND bn.status = 'PENDING'")
    int cancelPendingNotificationsForJob(@Param("jobId") Long jobId);

    @Query("SELECT COUNT(bn) > 0 FROM BillingNotification bn " +
            "WHERE bn.jobId = :jobId AND bn.scheduledDate = :scheduledDate " +
            "AND bn.noticeType = :noticeType AND bn.recipientEmail = :recipientEmail " +
            "AND bn.status != 'CANCELLED'")
    boolean existsByJobIdAndScheduledDateAndNoticeTypeAndRecipientEmail(
            @Param("jobId") Long jobId,
            @Param("scheduledDate") LocalDate scheduledDate,
            @Param("noticeType") String noticeType,
            @Param("recipientEmail") String recipientEmail
    );

    List<BillingNotification> findByJobIdAndStatus(Long jobId, String status);

    @Modifying
    @Query("DELETE FROM BillingNotification bn WHERE bn.jobId = :jobId AND bn.status = 'PENDING'")
    void deletePendingNotificationsForJob(@Param("jobId") Long jobId);
}