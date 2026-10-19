package com.example.eac_backend_project.repository;
import com.example.eac_backend_project.entity.inventory.InventoryPurchaseLine;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
public interface InventoryPurchaseLineRepository extends JpaRepository<InventoryPurchaseLine,Long> {
 List<InventoryPurchaseLine> findByInventoryRequestIdOrderById(Long id);
 @Query("select coalesce(sum(l.receivedQuantity-l.issuedQuantity),0) from InventoryPurchaseLine l where l.productId=:id")
 int reservedStock(@Param("id") Long id);
}
