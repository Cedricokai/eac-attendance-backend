package com.example.eac_backend_project.repository;

import com.example.eac_backend_project.entity.inventory.InventoryRequest;
import com.example.eac_backend_project.entity.inventory.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryRequestRepository extends JpaRepository<InventoryRequest, Long> {

    List<InventoryRequest> findByRequestedBy(String requestedBy);

    List<InventoryRequest> findByStatus(RequestStatus status);

    List<InventoryRequest> findByDepartment(String department);

    List<InventoryRequest> findByProjectNameContainingIgnoreCase(String projectName);

    InventoryRequest findByRequestNumber(String requestNumber);

    List<InventoryRequest> findByStatusIn(List<RequestStatus> statuses);
}
