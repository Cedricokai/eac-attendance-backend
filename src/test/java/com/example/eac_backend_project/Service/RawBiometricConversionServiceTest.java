package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.dto.BiometricRecordDTO;
import com.example.eac_backend_project.entity.attendance.*;
import com.example.eac_backend_project.entity.settings.Category;
import com.example.eac_backend_project.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RawBiometricConversionServiceTest {
    final RawBiometricEventRepository repository = mock(RawBiometricEventRepository.class);
    final EmployeeRepository employees = mock(EmployeeRepository.class);
    final AttendanceService attendance = mock(AttendanceService.class);
    final RawBiometricConversionService service = new RawBiometricConversionService(repository, employees, attendance, new ObjectMapper());
    final Employee employee = new Employee();
    RawBiometricConversionServiceTest() {
        employee.setId(7L);
        when(employees.findByIdAndActiveTrue(7L)).thenReturn(Optional.of(employee));
    }
    RawBiometricEvent event(long id, String action, String timestamp) {
        RawBiometricEvent event = new RawBiometricEvent(); event.setId(id); event.setEmployee(employee);
        event.setRawPayload("{}"); event.setPunchType(action); event.setEventTimestamp(LocalDateTime.parse(timestamp));
        return event;
    }
    Attendance saved(String date) {
        Attendance row = new Attendance(); row.setId(99L); row.setEmployee(employee); row.setDate(LocalDate.parse(date));
        return row;
    }
    @Test void convertsOnePairAndLinksEverySelectedPunch() {
        var early = event(1, "SIGN ON", "2026-09-23T08:00:00");
        var duplicate = event(2, "SIGN ON", "2026-09-23T09:00:00");
        var out = event(3, "SIGN OFF", "2026-09-23T17:00:00");
        when(repository.findAllById(any())).thenReturn(List.of(duplicate, out, early));
        when(attendance.processBiometricRecords(anyList())).thenReturn(List.of(saved("2026-09-23")));
        var result = service.convert(List.of(1L, 2L, 3L));
        assertEquals(3, result.get("convertedEvents"));
        ArgumentCaptor<List<BiometricRecordDTO>> captor = ArgumentCaptor.forClass(List.class);
        verify(attendance).processBiometricRecords(captor.capture());
        assertEquals(2, captor.getValue().size());
        assertTrue(captor.getValue().stream().anyMatch(dto -> dto.getTimestamp().equals(early.getEventTimestamp())));
        for (var event : List.of(early, duplicate, out)) { assertEquals(99L, event.getAttendance().getId()); assertEquals("PROCESSED", event.getProcessingStatus()); }
    }
    @Test void normalizesOriginalOnePassPayloadAndOvernightDate() {
        Category category = new Category(); category.setOvernightShift(true); employee.setCategory(category);
        when(attendance.getCategoryWorkStartTime(employee)).thenReturn("18:00");
        var event = new RawBiometricEvent(); event.setId(1L); event.setDeviceUserId("7");
        event.setRawPayload("{\"time\":\"2026/09/24-06:00:00\",\"signOn\":false}");
        var preview = service.preview(event);
        assertEquals("SIGN OFF", preview.get("punchType"));
        assertEquals(LocalDate.of(2026, 9, 23), preview.get("attendanceDate"));
        assertNull(preview.get("conversionIssue"));
        when(repository.findAllById(any())).thenReturn(List.of(event));
        when(attendance.processBiometricRecords(anyList())).thenReturn(List.of(saved("2026-09-23")));
        service.convert(List.of(1L));
        assertEquals(99L, event.getAttendance().getId());
    }
    @Test void validatesAllEventsBeforeSavingAnything() {
        var valid = event(1, "SIGN ON", "2026-09-23T08:00:00");
        var invalid = event(2, "UNKNOWN", "2026-09-23T08:00:00");
        when(repository.findAllById(any())).thenReturn(List.of(valid, invalid));
        assertThrows(ResponseStatusException.class, () -> service.convert(List.of(1L, 2L)));
        verify(attendance, never()).processBiometricRecords(anyList());
        verify(repository, never()).save(any());
    }
    @Test void linkedEventsAreSkippedAndMissingIdsRejected() {
        var linked = event(1, "SIGN ON", "2026-09-23T08:00:00"); linked.setAttendance(saved("2026-09-23"));
        when(repository.findAllById(any())).thenReturn(List.of(linked));
        assertEquals(1, service.convert(List.of(1L)).get("skippedEvents"));
        assertThrows(ResponseStatusException.class, () -> service.convert(List.of(1L, 2L)));
        verifyNoInteractions(attendance);
    }
}
