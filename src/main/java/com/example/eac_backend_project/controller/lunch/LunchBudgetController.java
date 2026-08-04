package com.example.eac_backend_project.controller.lunch;

import com.example.eac_backend_project.Service.lunch.LunchBudgetService;
import com.example.eac_backend_project.entity.lunch.LunchBudget;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/lunch/budget")
@RequiredArgsConstructor
public class LunchBudgetController {

    private final LunchBudgetService budgetService;

    @GetMapping("/current")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<LunchBudget> getCurrentBudget(@RequestParam String budgetType) {
        Optional<LunchBudget> budget = budgetService.getCurrentBudget(budgetType);
        return budget.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<LunchBudget> createBudget(@RequestBody LunchBudget budget) {
        return ResponseEntity.ok(budgetService.saveBudget(budget));
    }
}