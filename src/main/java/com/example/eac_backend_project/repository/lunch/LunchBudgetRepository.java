package com.example.eac_backend_project.repository.lunch;

import com.example.eac_backend_project.entity.lunch.LunchBudget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LunchBudgetRepository extends JpaRepository<LunchBudget, Long> {

    Optional<LunchBudget> findTopByBudgetTypeOrderByEffectiveDateDesc(String budgetType);
    List<LunchBudget> findByBudgetType(String budgetType);
}