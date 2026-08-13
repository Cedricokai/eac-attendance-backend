package com.example.eac_backend_project.Service.lunch;

import com.example.eac_backend_project.entity.lunch.LunchMeal;
import com.example.eac_backend_project.repository.lunch.LunchMealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LunchMealService {

    private final LunchMealRepository mealRepository;

    public List<LunchMeal> getAllMeals() {
        return mealRepository.findAll();
    }

    public List<LunchMeal> getActiveMeals() {
        return mealRepository.findByActiveTrue();
    }

    public LunchMeal getMeal(Long id) {
        return mealRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Meal not found"));
    }

    public LunchMeal saveMeal(LunchMeal meal) {
        return mealRepository.save(meal);
    }

    public void deleteMeal(Long id) {
        mealRepository.deleteById(id);
    }
}