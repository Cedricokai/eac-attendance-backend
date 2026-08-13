package com.example.eac_backend_project.repository;

import com.example.eac_backend_project.entity.attendance.AttendanceRequest;
import com.example.eac_backend_project.entity.attendance.AttendanceRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface AttendanceRequestRepository extends JpaRepository<AttendanceRequest, Long> {

    boolean existsByEmployeeIdAndDateAndStatus(Long employeeId, LocalDate date, AttendanceRequestStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ar from AttendanceRequest ar where ar.id in :ids")
    List<AttendanceRequest> findAllByIdForUpdate(@Param("ids") Collection<Long> ids);

    @Query("""
        select ar from AttendanceRequest ar
        join ar.employee e
        where (:status is null or ar.status = :status)
          and (:employeeId is null or e.id = :employeeId)
          and (:startDate is null or ar.date >= :startDate)
          and (:endDate is null or ar.date <= :endDate)
          and (:supervisorId is null or e.supervisor.id = :supervisorId)
          and (:search is null or lower(concat(coalesce(e.firstName, ''), ' ', coalesce(e.lastName, ''))) like lower(concat('%', :search, '%'))
               or lower(coalesce(e.employeeId, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(ar.reason, '')) like lower(concat('%', :search, '%')))
        """)
    Page<AttendanceRequest> search(
            @Param("status") AttendanceRequestStatus status,
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("search") String search,
            @Param("supervisorId") Long supervisorId,
            Pageable pageable
    );
}
