package com.example.eac_backend_project.repository;

import com.example.eac_backend_project.entity.attendance.RuleSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RuleSetRepository extends JpaRepository<RuleSet, Long> {
    List<RuleSet> findByIsActiveTrue();
}