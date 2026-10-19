package com.example.eac_backend_project.repository;
import com.example.eac_backend_project.entity.inventory.PpeReplacementPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface PpeReplacementPolicyRepository extends JpaRepository<PpeReplacementPolicy, Long> {
    Optional<PpeReplacementPolicy> findByPpeTypeIgnoreCase(String type);
    List<PpeReplacementPolicy> findAllByOrderByPpeTypeAsc();
}
