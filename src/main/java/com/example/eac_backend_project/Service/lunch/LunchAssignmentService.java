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

    private static final List<String> DAYS = Arrays.asList("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY");

    public List<LunchAssignment> getAllActiveAssignments() {
        return assignmentRepository.findAllActive();
    }

    public Optional<LunchAssignment> getActiveAssignmentForEmployee(Long employeeId, LocalDate date) {
        return assignmentRepository.findActiveAssignmentByEmployeeAndDate(employeeId, date);
    }

    public List<LunchAssignment> getAssignmentsForDate(LocalDate date) {
        return assignmentRepository.findActiveAssignmentsForDate(date);
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
        Employee employee = new Employee();
        employee.setId(employeeId);

        Map<String, Long> daysMap = dto.getDays() != null ? dto.getDays() : new HashMap<>();

        for (int i = 0; i < 5; i++) {
            LocalDate currentDate = monday.plusDays(i);
            String dayName = currentDate.getDayOfWeek().name();
            Long mealId = daysMap.get(dayName);

            Optional<LunchAssignment> existingOpt = assignmentRepository
                    .findActiveAssignmentByEmployeeAndDate(employeeId, currentDate);

            if (mealId == null || mealId == 0) {
                existingOpt.ifPresent(assignment -> {
                    assignment.setStatus("EXPIRED");
                    assignmentRepository.save(assignment);
                });
            } else {
                LunchMeal meal = mealRepository.findById(mealId)
                        .orElseThrow(() -> new RuntimeException("Meal not found with id: " + mealId));

                if (existingOpt.isPresent()) {
                    LunchAssignment existing = existingOpt.get();
                    existing.setMeal(meal);
                    existing.setStartDate(currentDate);
                    existing.setEndDate(currentDate);
                    existing.setStatus("ACTIVE");
                    if (existing.getAssignedBy() == null) {
                        existing.setAssignedBy(user);
                    }
                    assignmentRepository.save(existing);
                } else {
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
    }

    @Transactional
    public void saveWeeklyAssignmentForEmployee(WeeklyAssignmentDTO dto, User user) {
        LocalTime now = LocalTime.now();
        LocalDate today = LocalDate.now();
        LocalDate monday = dto.getStartDate();
        while (monday.getDayOfWeek().getValue() != 1) {
            monday = monday.minusDays(1);
        }
        Map<String, Long> daysMap = dto.getDays() != null ? dto.getDays() : new HashMap<>();
        for (Map.Entry<String, Long> entry : daysMap.entrySet()) {
            String dayName = entry.getKey();
            int dayIndex = DAYS.indexOf(dayName);
            if (dayIndex == -1) continue;
            LocalDate date = monday.plusDays(dayIndex);
            if (date.equals(today) && now.isAfter(LocalTime.of(8, 0))) {
                Optional<LunchAssignment> existing = assignmentRepository
                        .findActiveAssignmentByEmployeeAndDate(dto.getEmployeeId(), date);
                Long oldMealId = existing.map(a -> a.getMeal() != null ? a.getMeal().getId() : null).orElse(null);
                Long newMealId = entry.getValue();
                if (!Objects.equals(oldMealId, newMealId)) {
                    throw new RuntimeException("Cannot change today's meal after 8:00 AM");
                }
            }
        }
        saveWeeklyAssignment(dto, user);
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