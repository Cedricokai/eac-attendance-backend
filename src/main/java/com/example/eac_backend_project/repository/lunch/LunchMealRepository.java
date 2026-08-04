package com.example.eac_backend_project.repository.lunch;

import com.example.eac_backend_project.entity.lunch.LunchMeal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LunchMealRepository extends JpaRepository<LunchMeal, Long> {

    List<LunchMeal> findByActiveTrue();
    List<LunchMeal> findByCategory(String category);
}