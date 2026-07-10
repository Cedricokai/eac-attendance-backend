package com.example.eac_backend_project.repository;

import com.example.eac_backend_project.entity.inventory.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    // Find by status
    List<PurchaseOrder> findByStatus(String status);

    // Find by storeNotified = true
    List<PurchaseOrder> findByStoreNotifiedTrue();

    // Find by storeNotified = false and status in ORDERED or DELIVERED
    @Query("SELECT po FROM PurchaseOrder po WHERE po.storeNotified = false AND po.status IN ('ORDERED', 'DELIVERED')")
    List<PurchaseOrder> findPendingStoreNotifications();

    // Find orders for store (storeNotified = true or status in specific states)
    @Query("SELECT DISTINCT po FROM PurchaseOrder po WHERE " +
            "po.storeNotified = true OR " +
            "po.status IN ('DELIVERED', 'RECEIVED', 'PROCUREMENT_RECEIVED')")
    List<PurchaseOrder> findOrdersForStore();

    // Find by status in list
    List<PurchaseOrder> findByStatusIn(List<String> statuses);

    // Search by supplier name
    List<PurchaseOrder> findBySupplierNameContainingIgnoreCase(String searchTerm);

    // Find by expected delivery date range
    List<PurchaseOrder> findByExpectedDeliveryDateBetween(LocalDateTime start, LocalDateTime end);

    // Find by purchase order number
    Optional<PurchaseOrder> findByPurchaseOrderNumber(String purchaseOrderNumber);

    // Find by storeNotified (boolean)
    List<PurchaseOrder> findByStoreNotified(boolean storeNotified);
}