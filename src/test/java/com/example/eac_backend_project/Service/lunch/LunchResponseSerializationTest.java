package com.example.eac_backend_project.Service.lunch;

import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.inventory.User;
import com.example.eac_backend_project.entity.inventory.Page;
import com.example.eac_backend_project.entity.lunch.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class LunchResponseSerializationTest {
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    private User userWithCircularPages() {
        User user = new User(); user.setId(1L); user.setPassword("must-not-be-returned");
        Page page = new Page(); page.setId(2L);
        // Singleton sets avoid calling Lombok's recursive hashCode while constructing the graph.
        user.setAccessiblePages(Collections.singleton(page));
        page.setUsers(Collections.singleton(user));
        return user;
    }

    @Test void assignmentListProducesCompleteJsonWithoutUserGraph() throws Exception {
        Employee employee = new Employee(); employee.setId(10L); employee.setFirstName("Ama");
        LunchMeal meal = new LunchMeal(); meal.setId(2L); meal.setName("Banku");
        LunchAssignment assignment = new LunchAssignment(); assignment.setId(3L);
        assignment.setEmployee(employee); assignment.setMeal(meal); assignment.setAssignedBy(userWithCircularPages());
        assignment.setStartDate(LocalDate.of(2026, 9, 14)); assignment.setEndDate(LocalDate.of(2026, 9, 18));
        String json = mapper.writeValueAsString(List.of(assignment));
        var result = mapper.readTree(json).get(0);
        assertEquals("Banku", result.path("meal").path("name").asText());
        assertEquals(10L, result.path("employee").path("id").asLong());
        assertFalse(result.has("assignedBy"));
        assertFalse(json.contains("password"));
        assertTrue(json.length() < 10000);
    }

    @Test void servingListProducesCompleteJsonWithoutUserGraph() throws Exception {
        Employee employee = new Employee(); employee.setId(10L);
        LunchServing serving = new LunchServing(); serving.setEmployee(employee);
        serving.setServedBy(userWithCircularPages()); serving.setLocation("MKV");
        String json = mapper.writeValueAsString(List.of(serving));
        var result = mapper.readTree(json).get(0);
        assertEquals("MKV", result.path("location").asText());
        assertFalse(result.has("servedBy"));
        assertFalse(json.contains("password"));
    }
}
