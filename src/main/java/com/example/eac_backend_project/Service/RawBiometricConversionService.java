package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.dto.BiometricRecordDTO;
import com.example.eac_backend_project.entity.attendance.*;
import com.example.eac_backend_project.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class RawBiometricConversionService {
    private final RawBiometricEventRepository repository;
    private final EmployeeRepository employees;
    private final AttendanceService attendance;
    private final ObjectMapper mapper;
    public RawBiometricConversionService(RawBiometricEventRepository repository, EmployeeRepository employees,
                                        AttendanceService attendance, ObjectMapper mapper) {
        this.repository = repository; this.employees = employees; this.attendance = attendance; this.mapper = mapper;
    }
    record Punch(RawBiometricEvent event, Employee employee, BiometricRecordDTO dto, LocalDate date) {}

    private Punch normalize(RawBiometricEvent event) {
        try {
            JsonNode payload = mapper.readTree(event.getRawPayload());
            if (payload == null) payload = mapper.createObjectNode();
            Long employeeId = event.getEmployee() == null ? null : event.getEmployee().getId();
            if (employeeId == null && event.getDeviceUserId() != null) employeeId = Long.valueOf(event.getDeviceUserId());
            if (employeeId == null) throw new IllegalArgumentException("No matched employee");
            Employee employee = employees.findByIdAndActiveTrue(employeeId)
                    .orElseThrow(() -> new IllegalArgumentException("Employee is missing or inactive"));
            LocalDateTime timestamp = event.getEventTimestamp();
            if (timestamp == null) {
                String value = payload.path("time").asText(payload.path("timestamp").asText(""));
                try { timestamp = LocalDateTime.parse(value); }
                catch (RuntimeException ignored) { timestamp = LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy/MM/dd-HH:mm:ss")); }
            }
            String action = event.getPunchType();
            if ((action == null || action.isBlank()) && payload.path("signOn").isBoolean())
                action = payload.get("signOn").asBoolean() ? "SIGN ON" : "SIGN OFF";
            if (action == null) action = payload.path("action").asText("");
            action = action.trim().toUpperCase(Locale.ROOT);
            if (!action.equals("SIGN ON") && !action.equals("SIGN OFF"))
                throw new IllegalArgumentException("Punch must be SIGN ON or SIGN OFF");
            LocalDate date = timestamp.toLocalDate();
            if (action.equals("SIGN OFF") && employee.getCategory() != null
                    && Boolean.TRUE.equals(employee.getCategory().getOvernightShift())
                    && timestamp.toLocalTime().isBefore(LocalTime.parse(attendance.getCategoryWorkStartTime(employee))))
                date = date.minusDays(1);
            BiometricRecordDTO dto = new BiometricRecordDTO();
            dto.setEmployeeId(employeeId); dto.setTimestamp(timestamp); dto.setAction(action);
            dto.setEmployeeName(event.getRawEmployeeName()); dto.setAuthenticationMethod(event.getVerificationMethod());
            return new Punch(event, employee, dto, date);
        } catch (Exception error) {
            throw new IllegalArgumentException("Event #" + event.getId() + ": " + error.getMessage(), error);
        }
    }

    public Map<String,Object> preview(RawBiometricEvent event) {
        Map<String,Object> result = new LinkedHashMap<>();
        if (event.getAttendance() != null) {
            result.put("savedNormalHours", event.getAttendance().getMinimumHour());
            result.put("savedOvertimeHours", event.getAttendance().getOvertime());
        }
        try {
            Punch punch = normalize(event);
            result.put("employeeId", punch.employee().getId());
            result.put("eventTimestamp", punch.dto().getTimestamp());
            result.put("punchType", punch.dto().getAction());
            result.put("attendanceDate", punch.date());
            result.put("categoryName", punch.employee().getCategory() == null ? null : punch.employee().getCategory().getName());
            result.put("conversionIssue", event.getAttendance() != null ? "Already linked to attendance" : null);
        } catch (IllegalArgumentException error) { result.put("conversionIssue", error.getMessage()); }
        return result;
    }

    @Transactional
    public Map<String,Object> convert(List<Long> ids) {
        if (ids == null || ids.isEmpty() || ids.size() > 500 || ids.stream().anyMatch(Objects::isNull))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select between 1 and 500 raw punches");
        Set<Long> uniqueIds = new LinkedHashSet<>(ids);
        List<RawBiometricEvent> events = repository.findAllById(uniqueIds);
        if (events.size() != uniqueIds.size()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Some selected events no longer exist. Refresh and retry.");
        List<Punch> punches = new ArrayList<>();
        int skipped = 0;
        for (RawBiometricEvent event : events) {
            if (event.getAttendance() != null) { skipped++; continue; }
            try { punches.add(normalize(event)); }
            catch (IllegalArgumentException error) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error.getMessage()); }
        }
        // One pair per employee/shift: earliest arrival and latest departure.
        Map<String,Punch> endpoints = new LinkedHashMap<>();
        for (Punch punch : punches) {
            String key = punch.employee().getId() + ":" + punch.date() + ":" + punch.dto().getAction();
            Punch current = endpoints.get(key);
            if (current == null || (punch.dto().getAction().equals("SIGN ON")
                    ? punch.dto().getTimestamp().isBefore(current.dto().getTimestamp())
                    : punch.dto().getTimestamp().isAfter(current.dto().getTimestamp()))) endpoints.put(key, punch);
        }
        List<Attendance> saved = endpoints.isEmpty() ? List.of()
                : attendance.processBiometricRecords(endpoints.values().stream().map(Punch::dto).toList());
        Map<String,Attendance> byShift = new HashMap<>();
        saved.forEach(row -> byShift.put(row.getEmployee().getId() + ":" + row.getDate(), row));
        for (Punch punch : punches) {
            Attendance row = byShift.get(punch.employee().getId() + ":" + punch.date());
            if (row == null) throw new IllegalStateException("Attendance conversion did not return the expected shift");
            RawBiometricEvent event = punch.event();
            event.setAttendance(row); event.setEmployee(punch.employee());
            event.setEventTimestamp(punch.dto().getTimestamp()); event.setPunchType(punch.dto().getAction());
            event.setProcessingStatus("PROCESSED"); event.setProcessedAt(LocalDateTime.now()); event.setErrorMessage(null);
            repository.save(event);
        }
        return Map.of("convertedEvents", punches.size(), "attendanceCount", saved.size(), "skippedEvents", skipped);
    }
}
