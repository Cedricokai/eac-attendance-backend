package com.example.eac_backend_project.repository;
import com.example.eac_backend_project.entity.inventory.InventoryReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface InventoryReceiptRepository extends JpaRepository<InventoryReceipt,Long> {
 List<InventoryReceipt> findByPurchaseLineIdOrderById(Long id);
}
