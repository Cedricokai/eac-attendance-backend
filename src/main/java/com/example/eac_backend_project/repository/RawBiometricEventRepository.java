package com.example.eac_backend_project.repository;

import com.example.eac_backend_project.entity.attendance.RawBiometricEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;

public interface RawBiometricEventRepository extends JpaRepository<RawBiometricEvent, Long> {
    @Query("SELECT r FROM RawBiometricEvent r WHERE " +
            "(:employeeId IS NULL OR r.employee.id = :employeeId OR r.deviceUserId = CAST(:employeeId AS string)) AND " +
            "(:start IS NULL OR COALESCE(r.eventTimestamp, r.receivedAt) >= :start) AND " +
            "(:end IS NULL OR COALESCE(r.eventTimestamp, r.receivedAt) <= :end) AND " +
            "(:source = '' OR r.sourceType = :source) AND (:status = '' OR r.processingStatus = :status) " +
            "ORDER BY COALESCE(r.eventTimestamp, r.receivedAt) DESC")
    Page<RawBiometricEvent> search(@Param("employeeId") Long employeeId,
                                   @Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end,
                                   @Param("source") String source,
                                   @Param("status") String status,
                                   Pageable pageable);
}