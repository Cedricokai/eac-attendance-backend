package com.example.eac_backend_project.repository;

import com.example.eac_backend_project.entity.directpurchase.DirectPurchase;
import com.example.eac_backend_project.entity.directpurchase.DirectPurchaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DirectPurchaseRepository extends JpaRepository<DirectPurchase, Long> {
    List<DirectPurchase> findByStatus(DirectPurchaseStatus status);
    List<DirectPurchase> findByRequesterContainingIgnoreCaseOrAccountantContainingIgnoreCase(String requester, String accountant);
}