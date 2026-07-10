package com.example.eac_backend_project.repository;

import com.example.eac_backend_project.entity.attendance.RuleSetItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RuleSetItemRepository extends JpaRepository<RuleSetItem, Long> {
    List<RuleSetItem> findByRuleSetId(Long ruleSetId);
}