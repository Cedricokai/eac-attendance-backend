package com.example.eac_backend_project.repository;

import com.example.eac_backend_project.entity.inventory.StoreReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StoreReceiptRepository extends JpaRepository<StoreReceipt, Long> {
}
