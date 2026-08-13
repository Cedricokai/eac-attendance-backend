package com.example.eac_backend_project.controller.lunch;

import com.example.eac_backend_project.Service.lunch.DailyMealService;
import com.example.eac_backend_project.Service.lunch.LunchMealService;
import com.example.eac_backend_project.dto.lunch.DailyMealDTO;
import com.example.eac_backend_project.entity.lunch.LunchMeal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lunch")
@RequiredArgsConstructor
public class LunchMealController {

    private final LunchMealService mealService;
    private final DailyMealService dailyMealService;

    // ----- Meals -----
    @GetMapping("/meals")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'KITCHEN', 'MANAGER', 'SUPERVISOR', 'EMPLOYEE')")
    public ResponseEntity<List<LunchMeal>> getAllMeals() {
        return ResponseEntity.ok(mealService.getAllMeals());
    }

    @GetMapping("/meals/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'KITCHEN', 'MANAGER', 'SUPERVISOR', 'EMPLOYEE')")
    public ResponseEntity<List<LunchMeal>> getActiveMeals() {
        return ResponseEntity.ok(mealService.getActiveMeals());
    }

    @PostMapping("/meals")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<LunchMeal> createMeal(@RequestBody LunchMeal meal) {
        return ResponseEntity.ok(mealService.saveMeal(meal));
    }

    @PutMapping("/meals/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<LunchMeal> updateMeal(@PathVariable Long id, @RequestBody LunchMeal meal) {
        meal.setId(id);
        return ResponseEntity.ok(mealService.saveMeal(meal));
    }

    @DeleteMapping("/meals/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<Void> deleteMeal(@PathVariable Long id) {
        mealService.deleteMeal(id);
        return ResponseEntity.noContent().build();
    }

    // ============================================================
    // DAILY MEAL OPTIONS (two meals per day, Mon-Fri)
    // ============================================================

    @GetMapping("/daily-meals")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<Map<String, DailyMealDTO>> getDailyMeals() {
        return ResponseEntity.ok(dailyMealService.getDailyMeals());
    }

    // ✅ FIXED: Return JSON response instead of Void
    @PostMapping("/daily-meals")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<Map<String, Object>> updateDailyMeals(@RequestBody Map<String, DailyMealDTO> dailyMeals) {
        dailyMealService.updateDailyMeals(dailyMeals);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Daily meal options saved successfully");
        return ResponseEntity.ok(response);
    }
}