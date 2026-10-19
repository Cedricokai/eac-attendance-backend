package com.example.eac_backend_project.Service.lunch;

import com.example.eac_backend_project.dto.lunch.WeeklyAssignmentDTO;
import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.inventory.User;
import com.example.eac_backend_project.entity.lunch.LunchAssignment;
import com.example.eac_backend_project.entity.lunch.LunchHistory;
import com.example.eac_backend_project.entity.lunch.LunchMeal;
import com.example.eac_backend_project.repository.lunch.LunchAssignmentRepository;
import com.example.eac_backend_project.repository.lunch.LunchHistoryRepository;
import com.example.eac_backend_project.repository.lunch.LunchMealRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.example.eac_backend_project.repository.EmployeeRepository;
import com.example.eac_backend_project.repository.lunch.DailyMealRepository;
import com.example.eac_backend_project.repository.lunch.LunchEmployeeLockRepository;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LunchAssignmentService {

    private final LunchAssignmentRepository assignmentRepository;
    private final LunchHistoryRepository historyRepository;
    private final LunchMealRepository mealRepository;
    private final ObjectMapper objectMapper;
    private final LunchSettingsService settingsService;
    private final EmployeeRepository employeeRepository;
    private final DailyMealRepository dailyMealRepository;
    private final LunchEmployeeLockRepository employeeLockRepository;
    private final org.springframework.context.ApplicationEventPublisher events;

    private static final List<String> DAYS = Arrays.asList("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY");

    public List<LunchAssignment> getAllActiveAssignments() {
        return assignmentRepository.findAllActive();
    }

    public Optional<LunchAssignment> getActiveAssignmentForEmployee(Long employeeId, LocalDate date) {
        return assignmentRepository.findActiveAssignmentByEmployeeAndDate(employeeId, date);
    }

    public List<LunchAssignment> getAssignmentsForDate(LocalDate date) {
        Map<Long, LunchAssignment> assignments = new LinkedHashMap<>();
        var priority = java.util.Comparator.comparing(LunchAssignment::getStartDate)
                .thenComparing(LunchAssignment::getId);
        for (var assignment : assignmentRepository.findActiveAssignmentsForDate(date)) {
            assignments.merge(assignment.getEmployee().getId(), assignment,
                    (a, b) -> priority.compare(a, b) >= 0 ? a : b);
        }
        return List.copyOf(assignments.values());
    }

    public List<LunchAssignment> getAssignmentsForEmployee(Long employeeId) {
        return assignmentRepository.findByEmployeeId(employeeId);
    }

    public WeeklyAssignmentDTO getWeeklyAssignment(Long employeeId, LocalDate startDate) {
        LocalDate monday = startDate;
        while (monday.getDayOfWeek().getValue() != 1) {
            monday = monday.minusDays(1);
        }

        WeeklyAssignmentDTO dto = new WeeklyAssignmentDTO();
        dto.setEmployeeId(employeeId);
        dto.setStartDate(monday);

        Map<String, Long> days = new HashMap<>();
        for (int i = 0; i < 5; i++) {
            LocalDate currentDate = monday.plusDays(i);
            String dayName = currentDate.getDayOfWeek().name();

            // ✅ Use findActiveAssignmentByEmployeeAndDate which checks startDate <= date <= endDate
            Optional<LunchAssignment> assignmentOpt = assignmentRepository
                    .findActiveAssignmentByEmployeeAndDate(employeeId, currentDate);

            Long mealId = assignmentOpt
                    .map(a -> a.getMeal() != null ? a.getMeal().getId() : null)
                    .orElse(null);
            days.put(dayName, mealId);
        }
        dto.setDays(days);
        return dto;
    }

    @Transactional
    public LunchAssignment createAssignment(LunchAssignment assignment, User assignedBy) {
        assignment.setAssignedBy(assignedBy);
        LunchAssignment saved = assignmentRepository.save(assignment);

        LunchHistory history = new LunchHistory();
        history.setEmployee(saved.getEmployee());
        history.setAction("ASSIGNED");
        Map<String, Object> details = new HashMap<>();
        details.put("assignmentId", saved.getId());
        details.put("start", saved.getStartDate());
        details.put("end", saved.getEndDate());
        history.setDetails(toJson(details));
        history.setPerformedBy(assignedBy);
        historyRepository.save(history);

        return saved;
    }

    @Transactional
    public LunchAssignment updateAssignment(Long id, LunchAssignment updated, User updatedBy) {
        LunchAssignment existing = assignmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        existing.setStartDate(updated.getStartDate());
        existing.setEndDate(updated.getEndDate());
        existing.setStatus(updated.getStatus());
        existing.setReason(updated.getReason());
        existing.setMeal(updated.getMeal());

        LunchAssignment saved = assignmentRepository.save(existing);

        LunchHistory history = new LunchHistory();
        history.setEmployee(saved.getEmployee());
        history.setAction("UPDATED");
        Map<String, Object> details = new HashMap<>();
        details.put("assignmentId", saved.getId());
        history.setDetails(toJson(details));
        history.setPerformedBy(updatedBy);
        historyRepository.save(history);

        return saved;
    }

    @Transactional
    public void deleteAssignment(Long id, User deletedBy) {
        LunchAssignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
        assignment.setDeleted(true);
        assignment.setStatus("EXPIRED");
        assignmentRepository.save(assignment);

        LunchHistory history = new LunchHistory();
        history.setEmployee(assignment.getEmployee());
        history.setAction("REMOVED");
        Map<String, Object> details = new HashMap<>();
        details.put("assignmentId", id);
        history.setDetails(toJson(details));
        history.setPerformedBy(deletedBy);
        historyRepository.save(history);
    }

    @Transactional
    public void suspendAssignment(Long id, User suspendedBy) {
        LunchAssignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
        assignment.setStatus("SUSPENDED");
        assignmentRepository.save(assignment);

        LunchHistory history = new LunchHistory();
        history.setEmployee(assignment.getEmployee());
        history.setAction("SUSPENDED");
        Map<String, Object> details = new HashMap<>();
        details.put("assignmentId", id);
        history.setDetails(toJson(details));
        history.setPerformedBy(suspendedBy);
        historyRepository.save(history);
    }

    @Transactional
    public void activateAssignment(Long id, User activatedBy) {
        LunchAssignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
        assignment.setStatus("ACTIVE");
        assignmentRepository.save(assignment);

        LunchHistory history = new LunchHistory();
        history.setEmployee(assignment.getEmployee());
        history.setAction("ACTIVATED");
        Map<String, Object> details = new HashMap<>();
        details.put("assignmentId", id);
        history.setDetails(toJson(details));
        history.setPerformedBy(activatedBy);
        historyRepository.save(history);
    }

    @Transactional
    public void saveWeeklyAssignment(WeeklyAssignmentDTO dto, User user) {
        if (user == null) {
            throw new RuntimeException("Authenticated user is required");
        }
        if (dto.getEmployeeId() == null) {
            throw new RuntimeException("Employee ID is required");
        }
        if (dto.getStartDate() == null) {
            throw new RuntimeException("Start date is required");
        }

        LocalDate monday = dto.getStartDate();
        while (monday.getDayOfWeek().getValue() != 1) {
            monday = monday.minusDays(1);
        }

        Long employeeId = dto.getEmployeeId();
        Employee employee = employeeLockRepository.lockEmployee(employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        Map<String, Long> daysMap = dto.getDays() != null ? dto.getDays() : new HashMap<>();

        var employeeChanges = new java.util.ArrayList<MealChangeNotification.Change>();
        for (int i = 0; i < 5; i++) {
            LocalDate currentDate = monday.plusDays(i);
            String dayName = currentDate.getDayOfWeek().name();
            if (!daysMap.containsKey(dayName)) continue;
            Long mealId = daysMap.get(dayName);

            Long oldMealId = assignmentRepository.findActiveAssignmentByEmployeeAndDate(employeeId, currentDate)
                    .map(a -> a.getMeal() == null ? null : a.getMeal().getId()).orElse(null);
            Long nextMealId = mealId == null || mealId == 0 ? null : mealId;
            if (Objects.equals(oldMealId, nextMealId)) continue;
            employeeChanges.add(new MealChangeNotification.Change(currentDate, mealName(oldMealId), mealName(nextMealId)));
            // Preserve the other dates when changing one day of a range assignment.
            java.util.List<LunchAssignment> overlapping = assignmentRepository.findActiveEmployeeAssignments(employeeId, currentDate);
            for (LunchAssignment old : overlapping) {
                if (old.getStartDate().isBefore(currentDate)) {
                    assignmentRepository.save(copyRange(old, old.getStartDate(), currentDate.minusDays(1)));
                }
                if (old.getEndDate().isAfter(currentDate)) {
                    assignmentRepository.save(copyRange(old, currentDate.plusDays(1), old.getEndDate()));
                }
                old.setStatus("EXPIRED");
                assignmentRepository.save(old);
            }
            if (mealId != null && mealId != 0) {
                LunchMeal meal = mealRepository.findById(mealId)
                        .orElseThrow(() -> new RuntimeException("Meal not found with id: " + mealId));
                LunchAssignment newAssignment = new LunchAssignment();
                newAssignment.setEmployee(employee);
                newAssignment.setStartDate(currentDate);
                newAssignment.setEndDate(currentDate);
                newAssignment.setAssignedBy(user);
                newAssignment.setStatus("ACTIVE");
                newAssignment.setMeal(meal);
                assignmentRepository.save(newAssignment);
            }
        }

        LunchHistory history = new LunchHistory();
        history.setEmployee(employee);
        history.setAction("WEEKLY_ASSIGNMENT");
        Map<String, Object> details = new HashMap<>();
        details.put("startDate", monday);
        details.put("days", daysMap);
        history.setDetails(toJson(details));
        history.setPerformedBy(user);
        historyRepository.save(history);
        if (!employeeChanges.isEmpty() && employee.getEmail() != null && !employee.getEmail().isBlank()) {
            events.publishEvent(new MealAssignmentNotification(employee.getEmail(), employee.getFirstName() + " " + employee.getLastName(), java.util.List.copyOf(employeeChanges)));
        }
    }

    @Transactional
    public void saveWeeklyAssignmentForEmployee(WeeklyAssignmentDTO dto, User user) {
        Employee employee = resolveEmployee(user);
        if (dto.getEmployeeId() != null && !employee.getId().equals(dto.getEmployeeId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only change your own meals");
        }
        dto.setEmployeeId(employee.getId());
        if (dto.getStartDate() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date is required");
        employeeLockRepository.lockEmployee(employee.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
        ZonedDateTime now = settingsService.now();
        MealChangePolicy policy = settingsService.getMealChangePolicy();
        LocalDate monday = dto.getStartDate();
        while (monday.getDayOfWeek().getValue() != 1) {
            monday = monday.minusDays(1);
        }
        Map<String, Long> daysMap = dto.getDays() != null ? dto.getDays() : new HashMap<>();
        for (Map.Entry<String, Long> entry : daysMap.entrySet()) {
            String dayName = entry.getKey();
            int dayIndex = DAYS.indexOf(dayName);
            if (dayIndex == -1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid weekday");
            LocalDate date = monday.plusDays(dayIndex);
            Long oldMealId = assignmentRepository.findActiveAssignmentByEmployeeAndDate(employee.getId(), date)
                    .map(a -> a.getMeal() == null ? null : a.getMeal().getId()).orElse(null);
            Long newMealId = entry.getValue() == null || entry.getValue() == 0 ? null : entry.getValue();
            if (Objects.equals(oldMealId, newMealId)) continue;
            if (!policy.editable(date, now)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "MONTHLY".equals(policy.frequency()) ? "Meal changes open on day " + policy.monthlyDay() + " of each month until " + policy.cutoffTime() + " (Ghana time), subject to meal deadlines." : "Meal changes for " + date + " closed at " + policy.deadline(date) + ". Refresh your meals.");
            }
            if (newMealId != null) {
                var menu = dailyMealRepository.findByDayOfWeek(dayName)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No meals configured for " + dayName));
                boolean offered = (menu.getMeal1() != null && menu.getMeal1().isActive() && newMealId.equals(menu.getMeal1().getId()))
                        || (menu.getMeal2() != null && menu.getMeal2().isActive() && newMealId.equals(menu.getMeal2().getId()));
                if (!offered) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose an available meal for " + dayName);
            }
        }
        // Only submitted changes are saved, so stale or omitted days cannot erase admin assignments.
        Map<String, Long> changes = new HashMap<>();
        var notificationChanges = new java.util.ArrayList<MealChangeNotification.Change>();
        for (var entry : daysMap.entrySet()) {
            LocalDate date = monday.plusDays(DAYS.indexOf(entry.getKey()));
            Long old = assignmentRepository.findActiveAssignmentByEmployeeAndDate(employee.getId(), date)
                    .map(a -> a.getMeal() == null ? null : a.getMeal().getId()).orElse(null);
            Long next = entry.getValue() == null || entry.getValue() == 0 ? null : entry.getValue();
            if (!Objects.equals(old, next)) {
                changes.put(entry.getKey(), next);
                notificationChanges.add(new MealChangeNotification.Change(date, mealName(old), mealName(next)));
            }
        }
        dto.setDays(changes);
        if (!changes.isEmpty()) {
            saveWeeklyAssignment(dto, user);
            String recipient = settingsService.getSettings().getOrDefault("mealChangeNotificationEmail", "");
            if (!recipient.isBlank()) {
                notificationChanges.sort(java.util.Comparator.comparing(MealChangeNotification.Change::date));
                events.publishEvent(new MealChangeNotification(recipient,
                        employee.getFirstName() + " " + employee.getLastName(),
                        employee.getEmployeeId(), java.util.List.copyOf(notificationChanges)));
            }
        }
    }

    private String mealName(Long id) {
        return id == null ? "No meal" : mealRepository.findById(id)
                .map(LunchMeal::getName).orElse("Meal #" + id);
    }

    private LunchAssignment copyRange(LunchAssignment original, LocalDate start, LocalDate end) {
        LunchAssignment copy = new LunchAssignment();
        copy.setEmployee(original.getEmployee());
        copy.setMeal(original.getMeal());
        copy.setAssignedBy(original.getAssignedBy());
        copy.setReason(original.getReason());
        copy.setStartDate(start);
        copy.setEndDate(end);
        copy.setStatus("ACTIVE");
        return copy;
    }

    private Employee resolveEmployee(User user) {
        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to view your meals");
        Employee employee = user.getEmployee();
        if (employee == null && user.getEmail() != null) employee = employeeRepository.findByEmail(user.getEmail()).orElse(null);
        if (employee == null || Boolean.FALSE.equals(employee.getActive())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No active employee profile is linked to your account. Please contact HR.");
        }
        return employee;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMyWeeklyAssignment(User user, LocalDate startDate) {
        Employee employee = resolveEmployee(user);
        WeeklyAssignmentDTO week = getWeeklyAssignment(employee.getId(), startDate);
        MealChangePolicy policy = settingsService.getMealChangePolicy();
        ZonedDateTime now = settingsService.now();
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", employee.getId());
        profile.put("firstName", employee.getFirstName());
        profile.put("lastName", employee.getLastName());
        profile.put("employeeId", employee.getEmployeeId());
        profile.put("department", employee.getDepartment());
        Map<String, Object> options = new LinkedHashMap<>();
        Map<String, String> deadlines = new LinkedHashMap<>();
        for (String day : DAYS) {
            var meals = new java.util.ArrayList<LunchMeal>();
            dailyMealRepository.findByDayOfWeek(day).ifPresent(menu -> {
                if (menu.getMeal1() != null && menu.getMeal1().isActive()) meals.add(menu.getMeal1());
                if (menu.getMeal2() != null && menu.getMeal2().isActive()
                        && meals.stream().noneMatch(m -> m.getId().equals(menu.getMeal2().getId()))) meals.add(menu.getMeal2());
            });
            options.put(day, meals);
            deadlines.put(day, policy.deadline(week.getStartDate().plusDays(DAYS.indexOf(day))).toOffsetDateTime().toString());
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("employee", profile);
        result.put("startDate", week.getStartDate());
        result.put("days", week.getDays());
        result.put("meals", mealRepository.findAll());
        result.put("options", options);
        result.put("policy", policy);
        result.put("deadlines", deadlines);
        result.put("serverNow", now.toOffsetDateTime().toString());
        return result;
    }

    public List<LunchAssignment> getAssignmentsForDateRange(LocalDate start, LocalDate end) {
        return assignmentRepository.findAssignmentsOverlappingDateRange(start, end);
    }

    private String toJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return data.toString();
        }
    }
}
