package com.example.eac_backend_project.repository;

import com.example.eac_backend_project.entity.inventory.Outgoing;
import com.example.eac_backend_project.entity.inventory.OutgoingRecordType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutgoingRepository extends JpaRepository<Outgoing, Long> {
    List<Outgoing> findByProductId(Long productId);
    List<Outgoing> findByUserName(String userName);
    List<Outgoing> findByLocation(String location);
    @Query("SELECT o FROM Outgoing o WHERE o.quantityMoved > 0")
    List<Outgoing> findOutgoingWithQuantities();


    List<Outgoing> findByRecordType(OutgoingRecordType type);
    List<Outgoing> findByRequestId(Long requestId);
    List<Outgoing> findByStatus(String status);

    List<Outgoing> findByStoreType(String storeType);
    List<Outgoing> findByReturnableAfterUse(Boolean returnableAfterUse);
    List<Outgoing> findByReturnStatus(String returnStatus);

}
