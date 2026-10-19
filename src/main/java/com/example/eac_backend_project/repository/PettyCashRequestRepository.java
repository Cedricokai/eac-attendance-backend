package com.example.eac_backend_project.repository;
import com.example.eac_backend_project.entity.finance.PettyCashRequest;
import org.springframework.data.jpa.repository.*;
import java.math.BigDecimal;
import java.util.List;
public interface PettyCashRequestRepository extends JpaRepository<PettyCashRequest,Long>{
 List<PettyCashRequest> findAllByOrderByCreatedAtDesc();
 List<PettyCashRequest> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);
 List<PettyCashRequest> findByStatusInOrderByProcessedAtDesc(List<String> statuses);
 List<PettyCashRequest> findByCycleIdOrderByCreatedAtDesc(Long cycleId);
 List<PettyCashRequest> findByCycleIdIsNullAndStatusIn(List<String> statuses);
 @Query("select coalesce(sum(p.approvedAmount),0) from PettyCashRequest p where p.status in ('APPROVED','VOUCHER_COMPLETED')") BigDecimal totalCommitted();
 @Query("select coalesce(sum(p.approvedAmount),0) from PettyCashRequest p where p.cycleId = :cycleId and p.status in ('APPROVED','VOUCHER_COMPLETED')") BigDecimal totalCommittedForCycle(Long cycleId);
}
