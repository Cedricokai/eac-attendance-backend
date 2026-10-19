package com.example.eac_backend_project.controller.lunch;

import com.example.eac_backend_project.Service.lunch.LunchPlanningService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/lunch/reports/meal-planning")
@RequiredArgsConstructor
public class LunchPlanningController {
    private final LunchPlanningService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'KITCHEN', 'MANAGER')")
    public ResponseEntity<LunchPlanningService.MealPlan> getPlan(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok().header("Cache-Control", "no-store").body(service.getPlan(date));
    }
}
