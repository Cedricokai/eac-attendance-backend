package com.example.eac_backend_project.Service.lunch;

import com.example.eac_backend_project.dto.lunch.WeeklyAssignmentDTO;
import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.inventory.User;
import com.example.eac_backend_project.entity.lunch.*;
import com.example.eac_backend_project.repository.EmployeeRepository;
import com.example.eac_backend_project.repository.lunch.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LunchMealPlanningTest {
    LunchAssignmentRepository assignments;
    LunchMealRepository meals;
    LunchSettingsService settings;
    DailyMealRepository menus;
    EmployeeRepository employees;
    LunchEmployeeLockRepository locks;
    LunchAssignmentService service;
    org.springframework.context.ApplicationEventPublisher events;
    LunchPlanningService planning;
    final LocalDate monday = LocalDate.of(2026, 9, 14);
    final List<LunchAssignment> stored = new ArrayList<>();
    Employee employee;
    User user;
    LunchMeal rice, banku;

    @BeforeEach void setup() {
        stored.clear();
        assignments = mock(LunchAssignmentRepository.class, CALLS_REAL_METHODS);
        meals = mock(LunchMealRepository.class);
        settings = mock(LunchSettingsService.class);
        menus = mock(DailyMealRepository.class);
        employees = mock(EmployeeRepository.class);
        locks = mock(LunchEmployeeLockRepository.class);
        events = mock(org.springframework.context.ApplicationEventPublisher.class);
        service = new LunchAssignmentService(assignments, mock(LunchHistoryRepository.class), meals,
                new ObjectMapper().findAndRegisterModules(), settings, employees, menus, locks, events);
        planning = new LunchPlanningService(assignments, menus, settings);
        employee = new Employee(); employee.setId(10L); employee.setFirstName("Ama"); employee.setLastName("Test"); employee.setActive(true);
        user = new User(); user.setId(1L); user.setEmployee(employee);
        rice = meal(1L, "Rice", "10.00"); banku = meal(2L, "Banku", "12.00");
        when(meals.findById(1L)).thenReturn(Optional.of(rice));
        when(meals.findById(2L)).thenReturn(Optional.of(banku));
        when(meals.findAll()).thenReturn(List.of(rice, banku));
        when(locks.lockEmployee(10L)).thenReturn(Optional.of(employee));
        when(settings.getMealChangePolicy()).thenReturn(new MealChangePolicy("17:00", 1, "Africa/Accra"));
        when(settings.getSettings()).thenReturn(Map.of("costPerLunch", "5.50"));
        at("2026-09-15T16:59:59Z");
        DailyMeal menu = new DailyMeal(); menu.setMeal1(rice); menu.setMeal2(banku);
        when(menus.findByDayOfWeek(anyString())).thenReturn(Optional.of(menu));
        when(assignments.save(any())).thenAnswer(call -> {
            LunchAssignment assignment = call.getArgument(0);
            if (assignment.getId() == null) { assignment.setId((long) stored.size() + 1); stored.add(assignment); }
            return assignment;
        });
        when(assignments.findActiveEmployeeAssignments(anyLong(), any())).thenAnswer(call -> {
            Long id = call.getArgument(0); LocalDate date = call.getArgument(1);
            return stored.stream().filter(a -> active(a, date) && a.getEmployee().getId().equals(id))
                    .sorted(Comparator.comparing(LunchAssignment::getStartDate).thenComparing(LunchAssignment::getId).reversed()).toList();
        });
        when(assignments.findActiveAssignmentsForDate(any())).thenAnswer(call -> stored.stream()
                .filter(a -> active(a, call.getArgument(0))).toList());
    }

    @Test void notifiesOnlyActualEmployeeChangesWithBeforeAndAfterMeals() {
        when(settings.getSettings()).thenReturn(Map.of("mealChangeNotificationEmail", "kitchen@example.com"));
        service.saveWeeklyAssignment(request(Map.of("WEDNESDAY", 1L)), user);
        verifyNoInteractions(events);
        service.saveWeeklyAssignmentForEmployee(request(Map.of("WEDNESDAY", 2L)), user);
        var captor = org.mockito.ArgumentCaptor.forClass(MealChangeNotification.class);
        verify(events).publishEvent(captor.capture());
        var event = captor.getValue();
        assertEquals("kitchen@example.com", event.recipient());
        assertEquals("Ama Test", event.employeeName());
        assertEquals(List.of(new MealChangeNotification.Change(monday.plusDays(2), "Rice", "Banku")), event.changes());
        clearInvocations(events);
        service.saveWeeklyAssignmentForEmployee(request(Map.of("WEDNESDAY", 2L)), user);
        verifyNoInteractions(events);
        service.saveWeeklyAssignmentForEmployee(request(Collections.singletonMap("WEDNESDAY", null)), user);
        verify(events).publishEvent(captor.capture());
        assertEquals("No meal", captor.getValue().changes().get(0).newMeal());
    }

    @Test void disabledAndRejectedChangesDoNotNotify() {
        service.saveWeeklyAssignmentForEmployee(request(Map.of("WEDNESDAY", 1L)), user);
        verifyNoInteractions(events);
        when(settings.getSettings()).thenReturn(Map.of("mealChangeNotificationEmail", "kitchen@example.com"));
        at("2026-09-16T12:00:00Z");
        assertThrows(ResponseStatusException.class, () -> service.saveWeeklyAssignmentForEmployee(request(Map.of("WEDNESDAY", 2L)), user));
        verifyNoInteractions(events);
    }

    @Test void dailyListAndPortalResolveOverlappingAssignmentsToSameMeal() {
        service.saveWeeklyAssignment(request(Map.of("WEDNESDAY", 1L)), user);
        LunchAssignment replacement = new LunchAssignment();
        replacement.setEmployee(employee); replacement.setMeal(banku);
        replacement.setStartDate(monday.plusDays(2)); replacement.setEndDate(monday.plusDays(2));
        replacement.setStatus("ACTIVE"); assignments.save(replacement);
        assertEquals(2L, selection("WEDNESDAY"));
        var daily = service.getAssignmentsForDate(monday.plusDays(2));
        assertEquals(1, daily.size());
        assertEquals(2L, daily.get(0).getMeal().getId());
    }

    private boolean active(LunchAssignment a, LocalDate date) {
        return "ACTIVE".equals(a.getStatus()) && !a.isDeleted() && !a.getStartDate().isAfter(date) && !a.getEndDate().isBefore(date);
    }
    private LunchMeal meal(long id, String name, String price) {
        LunchMeal meal = new LunchMeal(); meal.setId(id); meal.setName(name); meal.setPrice(new BigDecimal(price)); return meal;
    }
    private void at(String instant) { when(settings.now()).thenReturn(ZonedDateTime.parse(instant).withZoneSameInstant(LunchSettingsService.ZONE)); }
    private WeeklyAssignmentDTO request(Map<String, Long> days) {
        WeeklyAssignmentDTO dto = new WeeklyAssignmentDTO(); dto.setEmployeeId(10L); dto.setStartDate(monday); dto.setDays(days); return dto;
    }
    private Long selection(String day) { return service.getWeeklyAssignment(10L, monday).getDays().get(day); }

    @Test void adminDefaultsAppearForSignedInEmployeeAndFeedNextDayTotals() {
        service.saveWeeklyAssignment(request(Map.of("WEDNESDAY", 1L, "THURSDAY", 2L)), user);
        Map<String, Object> portal = service.getMyWeeklyAssignment(user, monday);
        assertEquals(1L, ((Map<?, ?>) portal.get("days")).get("WEDNESDAY"));
        assertEquals(10L, ((Map<?, ?>) portal.get("employee")).get("id"));
        var plan = planning.getPlan(null);
        assertEquals(monday.plusDays(2), plan.date());
        assertEquals(1, plan.totalEmployees());
        assertEquals(new BigDecimal("10.00"), plan.estimatedCost());
        service.saveWeeklyAssignmentForEmployee(request(Map.of("WEDNESDAY", 2L)), user);
        var updated = planning.getPlan(null);
        assertEquals(1, updated.totalEmployees());
        assertEquals(new BigDecimal("12.00"), updated.estimatedCost());
        assertEquals(0, updated.meals().stream().filter(r -> r.mealId() == 1L).findFirst().orElseThrow().employeeCount());
        assertEquals(2L, selection("THURSDAY"));
    }

    @Test void exactDeadlineAndPastDatesCannotBeChangedEvenByDirectApiRequest() {
        service.saveWeeklyAssignment(request(Map.of("WEDNESDAY", 1L, "MONDAY", 1L)), user);
        at("2026-09-15T17:00:00Z");
        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> service.saveWeeklyAssignmentForEmployee(request(Map.of("WEDNESDAY", 2L)), user)).getStatusCode().value());
        assertEquals(1L, selection("WEDNESDAY"));
        assertThrows(ResponseStatusException.class, () -> service.saveWeeklyAssignmentForEmployee(request(Map.of("MONDAY", 2L)), user));
        int count = stored.size();
        service.saveWeeklyAssignmentForEmployee(request(Map.of("MONDAY", 1L, "WEDNESDAY", 1L)), user);
        assertEquals(count, stored.size());
    }

    @Test void submittedEmployeeIdCannotOverrideLoggedInIdentity() {
        var dto = request(Map.of("WEDNESDAY", 1L)); dto.setEmployeeId(99L);
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> service.saveWeeklyAssignmentForEmployee(dto, user)).getStatusCode().value());
        user.setEmployee(null);
        assertThrows(ResponseStatusException.class, () -> service.getMyWeeklyAssignment(user, monday));
        assertTrue(stored.isEmpty());
    }

    @Test void changingOneDayPreservesOtherDaysInRange() {
        LunchAssignment range = new LunchAssignment(); range.setEmployee(employee); range.setAssignedBy(user);
        range.setStartDate(monday); range.setEndDate(monday.plusDays(4)); range.setMeal(rice); assignments.save(range);
        service.saveWeeklyAssignmentForEmployee(request(Map.of("WEDNESDAY", 2L)), user);
        assertEquals(1L, selection("MONDAY")); assertEquals(1L, selection("TUESDAY"));
        assertEquals(2L, selection("WEDNESDAY")); assertEquals(1L, selection("THURSDAY")); assertEquals(1L, selection("FRIDAY"));
        assertEquals(1, planning.getPlan(null).totalEmployees());
    }

    @Test void totalsDeduplicateEmployeesAndExcludeInactiveEmployees() {
        service.saveWeeklyAssignment(request(Map.of("WEDNESDAY", 1L)), user);
        LunchAssignment duplicate = new LunchAssignment(); duplicate.setEmployee(employee); duplicate.setAssignedBy(user);
        duplicate.setStartDate(monday.plusDays(2)); duplicate.setEndDate(monday.plusDays(2)); duplicate.setMeal(banku); assignments.save(duplicate);
        assertEquals(1, planning.getPlan(null).totalEmployees());
        assertEquals(new BigDecimal("12.00"), planning.getPlan(null).estimatedCost());
        employee.setActive(false);
        assertEquals(0, planning.getPlan(null).totalEmployees());
    }

    @Test void employeeMustChooseOfferedMeal() {
        assertThrows(ResponseStatusException.class, () -> service.saveWeeklyAssignmentForEmployee(request(Map.of("WEDNESDAY", 99L)), user));
        assertTrue(stored.isEmpty());
    }

    @Test void sameDayPolicyHonorsGhanaTimeAndExactCutoff() {
        MealChangePolicy policy = new MealChangePolicy("08:00", 0, "Africa/Accra");
        LocalDate date = LocalDate.of(2026, 9, 16);
        assertTrue(policy.editable(date, ZonedDateTime.parse("2026-09-16T09:59:59+02:00")));
        assertFalse(policy.editable(date, ZonedDateTime.parse("2026-09-16T10:00:00+02:00")));
        assertFalse(policy.editable(date.minusDays(1), ZonedDateTime.parse("2026-09-16T00:01:00Z")));
    }
}
