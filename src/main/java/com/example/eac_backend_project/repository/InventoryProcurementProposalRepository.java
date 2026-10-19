package com.example.eac_backend_project.repository;
import com.example.eac_backend_project.entity.inventory.InventoryProcurementProposal;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface InventoryProcurementProposalRepository extends JpaRepository<InventoryProcurementProposal,Long> {
 List<InventoryProcurementProposal> findByInventoryRequestIdOrderByIdDesc(Long id);
 List<InventoryProcurementProposal> findByStatusOrderBySubmittedAtAsc(String status);
 @Query("select coalesce(sum(i.quantity),0) from InventoryProposalItem i where i.proposal.inventoryRequestId=:requestId and i.requestItemId=:itemId and i.proposal.status='AWAITING_ACCOUNTS'")
 int pendingQuantity(@Param("requestId") Long requestId,@Param("itemId") Long itemId);
}
