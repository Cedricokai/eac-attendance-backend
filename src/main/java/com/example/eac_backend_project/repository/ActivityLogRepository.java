package com.example.eac_backend_project.repository;

import com.example.eac_backend_project.entity.attendance.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    Page<ActivityLog> findAllByOrderByTimestampDesc(Pageable pageable);
    List<ActivityLog> findBySourceAndTimestampBetween(String source, LocalDateTime from, LocalDateTime to);
    List<ActivityLog> findByTypeAndTimestampBetween(String type, LocalDateTime from, LocalDateTime to);
    @Query("SELECT a FROM ActivityLog a WHERE " +
            "(:source IS NULL OR a.source = :source) AND " +
            "(:type IS NULL OR a.type = :type) AND " +
            "(CAST(:fromDate AS date) IS NULL OR a.timestamp >= :fromDate) AND " +
            "(CAST(:toDate AS date) IS NULL OR a.timestamp <= :toDate)")
    Page<ActivityLog> searchLogs(@Param("source") String source,
                                 @Param("type") String type,
                                 @Param("fromDate") LocalDateTime fromDate,
                                 @Param("toDate") LocalDateTime toDate,
                                 Pageable pageable);

    List<ActivityLog> findTop10ByOrderByTimestampDesc();

}