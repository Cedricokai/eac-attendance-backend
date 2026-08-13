package com.example.eac_backend_project.repository.lunch;

import com.example.eac_backend_project.entity.lunch.DailyMeal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DailyMealRepository extends JpaRepository<DailyMeal, Long> {

    Optional<DailyMeal> findByDayOfWeek(String dayOfWeek);

    List<DailyMeal> findAllByOrderByDayOfWeek();
}