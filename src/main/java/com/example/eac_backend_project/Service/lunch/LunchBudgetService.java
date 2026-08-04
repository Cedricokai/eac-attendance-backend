package com.example.eac_backend_project.Service.lunch;

import com.example.eac_backend_project.entity.lunch.LunchBudget;
import com.example.eac_backend_project.repository.lunch.LunchBudgetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LunchBudgetService {

    private final LunchBudgetRepository budgetRepository;

    public Optional<LunchBudget> getCurrentBudget(String budgetType) {
        return budgetRepository.findTopByBudgetTypeOrderByEffectiveDateDesc(budgetType);
    }

    public LunchBudget saveBudget(LunchBudget budget) {
        return budgetRepository.save(budget);
    }
}