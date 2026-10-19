package com.example.eac_backend_project.Service.lunch;

import com.example.eac_backend_project.entity.lunch.LunchAssignment;
import com.example.eac_backend_project.entity.lunch.LunchMeal;
import com.example.eac_backend_project.repository.lunch.LunchAssignmentRepository;
import com.example.eac_backend_project.repository.lunch.DailyMealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LunchPlanningService {
    private final LunchAssignmentRepository assignmentRepository;
    private final DailyMealRepository dailyMealRepository;
    private final LunchSettingsService settingsService;

    public record MealTotal(Long mealId, String mealName, int employeeCount, BigDecimal unitCost, BigDecimal estimatedCost) {}
    public record MealPlan(LocalDate date, String generatedAt, String deadline, boolean changesOpen,
                           String timezone, List<MealTotal> meals, int totalEmployees, BigDecimal estimatedCost) {}

    @Transactional(readOnly = true)
    public MealPlan getPlan(LocalDate requestedDate) {
        var now = settingsService.now();
        LocalDate date = requestedDate == null ? now.toLocalDate().plusDays(1) : requestedDate;
        var policy = settingsService.getMealChangePolicy();
        Map<Long, LunchAssignment> perEmployee = new HashMap<>();
        Comparator<LunchAssignment> priority = Comparator.comparing(LunchAssignment::getStartDate)
                .thenComparing(LunchAssignment::getId);
        for (LunchAssignment assignment : assignmentRepository.findActiveAssignmentsForDate(date)) {
            if (assignment.getEmployee() == null || !Boolean.TRUE.equals(assignment.getEmployee().getActive())) continue;
            perEmployee.merge(assignment.getEmployee().getId(), assignment,
                    (a, b) -> priority.compare(a, b) >= 0 ? a : b);
        }
        Map<Long, LunchMeal> meals = new HashMap<>();
        dailyMealRepository.findByDayOfWeek(date.getDayOfWeek().name()).ifPresent(menu -> {
            if (menu.getMeal1() != null && menu.getMeal1().isActive()) meals.put(menu.getMeal1().getId(), menu.getMeal1());
            if (menu.getMeal2() != null && menu.getMeal2().isActive()) meals.put(menu.getMeal2().getId(), menu.getMeal2());
        });
        Map<Long, Integer> counts = new HashMap<>();
        for (LunchAssignment assignment : perEmployee.values()) {
            LunchMeal meal = assignment.getMeal();
            if (meal == null) continue;
            meals.put(meal.getId(), meal);
            counts.merge(meal.getId(), 1, Integer::sum);
        }
        BigDecimal fallbackCost = new BigDecimal(settingsService.getSettings().get("costPerLunch"));
        List<MealTotal> rows = meals.values().stream().sorted(Comparator.comparing(LunchMeal::getName))
                .map(meal -> {
                    int count = counts.getOrDefault(meal.getId(), 0);
                    BigDecimal unit = meal.getPrice() == null ? fallbackCost : meal.getPrice();
                    return new MealTotal(meal.getId(), meal.getName(), count, unit, unit.multiply(BigDecimal.valueOf(count)));
                }).toList();
        return new MealPlan(date, now.toOffsetDateTime().toString(), policy.deadline(date).toOffsetDateTime().toString(),
                policy.editable(date, now), policy.timezone(), rows, counts.values().stream().mapToInt(Integer::intValue).sum(),
                rows.stream().map(MealTotal::estimatedCost).reduce(BigDecimal.ZERO, BigDecimal::add));
    }
}
