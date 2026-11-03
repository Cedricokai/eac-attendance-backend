package com.example.eac_backend_project.repository;

import com.example.eac_backend_project.entity.inventory.EmployeeIssuedItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeIssuedItemRepository extends JpaRepository<EmployeeIssuedItem, Long> {
    List<EmployeeIssuedItem> findAllByOrderByCreatedAtDesc();
    List<EmployeeIssuedItem> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);
}

