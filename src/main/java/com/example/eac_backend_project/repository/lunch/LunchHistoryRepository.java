package com.example.eac_backend_project.repository.lunch;

import com.example.eac_backend_project.entity.lunch.LunchHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LunchHistoryRepository extends JpaRepository<LunchHistory, Long> {
}