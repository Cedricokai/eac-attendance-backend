package com.example.eac_backend_project.Service.lunch;

import com.example.eac_backend_project.Service.UserService;
import com.example.eac_backend_project.controller.lunch.LunchAssignmentController;
import com.example.eac_backend_project.controller.lunch.LunchPlanningController;
import com.example.eac_backend_project.dto.lunch.WeeklyAssignmentDTO;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import java.time.LocalDate;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@SpringJUnitConfig(LunchMealAccessTest.Config.class)
class LunchMealAccessTest {
    @Configuration @EnableMethodSecurity static class Config {
        @Bean LunchAssignmentService assignments() { return mock(LunchAssignmentService.class); }
        @Bean LunchPlanningService planning() { return mock(LunchPlanningService.class); }
        @Bean UserService users() { return mock(UserService.class); }
        @Bean LunchAssignmentController controller(LunchAssignmentService s, UserService u) { return new LunchAssignmentController(s, u); }
        @Bean LunchPlanningController planningController(LunchPlanningService s) { return new LunchPlanningController(s); }
    }
    @Autowired LunchAssignmentController controller;
    @Autowired LunchPlanningController planningController;
    @Autowired LunchAssignmentService service;

    @Test @WithMockUser(roles = "EMPLOYEE") void employeeCannotBypassCutoffViaAdminEndpoint() {
        assertThrows(AccessDeniedException.class, () -> controller.createOrUpdateWeeklyAssignment(new WeeklyAssignmentDTO()));
        assertThrows(AccessDeniedException.class, () -> planningController.getPlan(null));
    }
    @Test @WithMockUser(roles = "EMPLOYEE") void employeeCanReadOwnMeals() {
        when(service.getMyWeeklyAssignment(any(), any())).thenReturn(Map.of("days", Map.of("MONDAY", 1)));
        assertEquals(200, controller.getMyWeeklyAssignment(LocalDate.of(2026, 9, 14)).getStatusCode().value());
    }
}
