package com.example.eac_backend_project.repository;

import com.example.eac_backend_project.entity.inventory.InventoryRequest;
import com.example.eac_backend_project.entity.inventory.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryRequestRepository extends JpaRepository<InventoryRequest, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select e from InventoryRequest e where e.id = :id")
    java.util.Optional<InventoryRequest> findForUpdate(@org.springframework.data.repository.query.Param("id") Long id);


    List<InventoryRequest> findByRequestedBy(String requestedBy);

    List<InventoryRequest> findByStatus(RequestStatus status);

    List<InventoryRequest> findByDepartment(String department);

    List<InventoryRequest> findByProjectNameContainingIgnoreCase(String projectName);

    InventoryRequest findByRequestNumber(String requestNumber);

    List<InventoryRequest> findByStatusIn(List<RequestStatus> statuses);
}
