package com.example.eac_backend_project.Service.lunch;

import com.example.eac_backend_project.dto.lunch.DailyMealDTO;
import com.example.eac_backend_project.entity.lunch.DailyMeal;
import com.example.eac_backend_project.entity.lunch.LunchMeal;
import com.example.eac_backend_project.repository.lunch.DailyMealRepository;
import com.example.eac_backend_project.repository.lunch.LunchMealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DailyMealService {

    private final DailyMealRepository dailyMealRepository;
    private final LunchMealRepository mealRepository;

    private static final List<String> DAYS = Arrays.asList("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY");

    public Map<String, DailyMealDTO> getDailyMeals() {
        Map<String, DailyMealDTO> result = new HashMap<>();
        for (String day : DAYS) {
            DailyMealDTO dto = new DailyMealDTO();
            dailyMealRepository.findByDayOfWeek(day).ifPresent(dailyMeal -> {
                if (dailyMeal.getMeal1() != null) dto.setMeal1Id(dailyMeal.getMeal1().getId());
                if (dailyMeal.getMeal2() != null) dto.setMeal2Id(dailyMeal.getMeal2().getId());
            });
            result.put(day, dto);
        }
        return result;
    }

    @Transactional
    public void updateDailyMeals(Map<String, DailyMealDTO> dailyMeals) {
        for (Map.Entry<String, DailyMealDTO> entry : dailyMeals.entrySet()) {
            String day = entry.getKey();
            if (!DAYS.contains(day)) {
                throw new IllegalArgumentException("Invalid day: " + day);
            }
            DailyMealDTO dto = entry.getValue();

            // Validate meal IDs exist
            LunchMeal meal1 = null;
            LunchMeal meal2 = null;
            if (dto.getMeal1Id() != null) {
                meal1 = mealRepository.findById(dto.getMeal1Id())
                        .orElseThrow(() -> new RuntimeException("Meal not found with id: " + dto.getMeal1Id()));
            }
            if (dto.getMeal2Id() != null) {
                meal2 = mealRepository.findById(dto.getMeal2Id())
                        .orElseThrow(() -> new RuntimeException("Meal not found with id: " + dto.getMeal2Id()));
            }

            DailyMeal dailyMeal = dailyMealRepository.findByDayOfWeek(day)
                    .orElse(new DailyMeal());
            dailyMeal.setDayOfWeek(day);
            dailyMeal.setMeal1(meal1);
            dailyMeal.setMeal2(meal2);
            dailyMealRepository.save(dailyMeal);
        }
    }
}