package com.example.eac_backend_project.repository;
import com.example.eac_backend_project.entity.inventory.EmployeeIssuedItem;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface EmployeeIssuedItemRepository extends JpaRepository<EmployeeIssuedItem, Long> {
    boolean existsByPpeTypeIgnoreCase(String type);
    List<EmployeeIssuedItem> findAllByOrderByIssueDateDescIdDesc();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from EmployeeIssuedItem i where i.id = :id")
    Optional<EmployeeIssuedItem> lockById(@Param("id") Long id);
}
