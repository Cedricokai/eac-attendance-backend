package com.example.eac_backend_project.repository;

import com.example.eac_backend_project.entity.finance.PettyCashCycle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface PettyCashCycleRepository extends JpaRepository<PettyCashCycle, Long> {
    Optional<PettyCashCycle> findFirstByStatusOrderByOpenedAtDesc(String status);
    List<PettyCashCycle> findAllByOrderByOpenedAtDesc();
}
