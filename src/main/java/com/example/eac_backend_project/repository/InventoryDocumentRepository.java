package com.example.eac_backend_project.repository;

import com.example.eac_backend_project.entity.inventory.InventoryDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InventoryDocumentRepository extends JpaRepository<InventoryDocument, Long> {
    List<InventoryDocument> findAllByOrderByUploadedAtDesc();
}
