package com.example.eac_backend_project.Service.lunch;

import java.time.LocalDate;
import java.util.List;

/** Immutable snapshot; never expose a managed entity to the asynchronous listener. */
public record MealChangeNotification(String recipient, String employeeName,
                                     String employeeNumber, List<Change> changes) {
    public record Change(LocalDate date, String previousMeal, String newMeal) {}
}
