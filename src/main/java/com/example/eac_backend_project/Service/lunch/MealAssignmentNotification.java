package com.example.eac_backend_project.Service.lunch;
import java.util.List;
public record MealAssignmentNotification(String recipient, String employeeName, List<MealChangeNotification.Change> changes) {}
