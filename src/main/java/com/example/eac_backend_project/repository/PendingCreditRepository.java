package com.example.eac_backend_project.repository;

import com.example.eac_backend_project.entity.attendance.PendingCredit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface PendingCreditRepository extends JpaRepository<PendingCredit, Long> {

    @Query("SELECT pc FROM PendingCredit pc WHERE pc.employee.id IN :employeeIds AND pc.status = 'PENDING'")
    List<PendingCredit> findPendingCreditsByEmployeeIds(@Param("employeeIds") List<Long> employeeIds);

    @Query("SELECT pc FROM PendingCredit pc WHERE pc.employee.id = :employeeId AND pc.status = 'PENDING'")
    List<PendingCredit> findPendingCreditsByEmployeeId(@Param("employeeId") Long employeeId);

    @Query("SELECT pc FROM PendingCredit pc WHERE pc.employee.id IN :employeeIds AND pc.status = 'PENDING' " +
            "AND (pc.appliedToPeriod IS NULL OR pc.appliedToPeriod.id IS NULL)")
    List<PendingCredit> findUnappliedPendingCreditsByEmployeeIds(@Param("employeeIds") List<Long> employeeIds);

    @Modifying
    @Transactional
    @Query("UPDATE PendingCredit pc SET pc.status = 'APPLIED', pc.appliedToPeriod.id = :periodId, pc.appliedDate = CURRENT_TIMESTAMP " +
            "WHERE pc.id IN :creditIds AND pc.status = 'PENDING'")
    int markCreditsAsApplied(@Param("creditIds") List<Long> creditIds, @Param("periodId") Long periodId);

    @Query("SELECT pc FROM PendingCredit pc WHERE pc.status = 'PENDING' AND pc.createdDate <= :cutoffDate")
    List<PendingCredit> findOldPendingCredits(@Param("cutoffDate") LocalDate cutoffDate);

    // Change this to return ALL credits (not just PENDING)
    @Query("SELECT pc FROM PendingCredit pc ORDER BY pc.createdDate DESC")
    List<PendingCredit> findAllPendingCredits();

    @Modifying
    @Query("UPDATE PendingCredit c SET c.status = 'PENDING', c.appliedToPeriod = NULL WHERE c.appliedToPeriod.id = :periodId AND c.status = 'APPLIED'")
    int revertAppliedCreditsByPeriod(@Param("periodId") Long periodId);
}