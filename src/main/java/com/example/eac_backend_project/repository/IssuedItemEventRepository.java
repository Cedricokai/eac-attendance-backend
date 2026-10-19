package com.example.eac_backend_project.repository;
import com.example.eac_backend_project.entity.inventory.IssuedItemEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface IssuedItemEventRepository extends JpaRepository<IssuedItemEvent, Long> {
    List<IssuedItemEvent> findByItemIdOrderByIdAsc(Long itemId);
}
