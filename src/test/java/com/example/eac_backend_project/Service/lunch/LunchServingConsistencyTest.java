package com.example.eac_backend_project.Service.lunch;

import com.example.eac_backend_project.entity.attendance.Attendance;
import com.example.eac_backend_project.entity.lunch.*;
import com.example.eac_backend_project.repository.AttendanceRepository;
import com.example.eac_backend_project.repository.lunch.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class LunchServingConsistencyTest {
    @Test void manualPresenceCanBeServedButAbsentAndUnassignedCannot() {
        var servings = mock(LunchServingRepository.class);
        var assignments = mock(LunchAssignmentRepository.class);
        var histories = mock(LunchHistoryRepository.class);
        var attendance = mock(AttendanceRepository.class);
        var service = new LunchServingService(servings, assignments, histories, attendance);
        var date = LocalDate.of(2026, 9, 16);
        var assignment = new LunchAssignment(); assignment.setMeal(new LunchMeal());
        when(assignments.findActiveAssignmentByEmployeeAndDate(10L, date)).thenReturn(Optional.of(assignment));
        var present = new Attendance(); present.setStatus("Present");
        when(attendance.findByEmployeeIdAndDate(10L, date)).thenReturn(Optional.of(present));
        when(servings.save(any())).thenAnswer(call -> call.getArgument(0));
        assertNotNull(service.serveLunch(10L, date, null, null, null, false, "MKV"));
        present.setStatus("Absent");
        assertThrows(RuntimeException.class, () -> service.serveLunch(10L, date, null, null, null, true, "MKV"));
        present.setStatus("Present"); assignment.setMeal(null);
        assertThrows(RuntimeException.class, () -> service.serveLunch(10L, date, null, null, null, true, "MKV"));
        verify(servings, times(1)).save(any());
    }

    @Test void attendanceStatusAndCheckInUseSamePresenceRule() {
        var attendance = new Attendance();
        for (String status : new String[]{"Present", "PRESENT", "Late", "Holiday Present", "WEEKEND_PRESENT"}) {
            attendance.setStatus(status); assertTrue(LunchAttendancePolicy.isPresent(attendance));
        }
        attendance.setStatus("Absent"); assertFalse(LunchAttendancePolicy.isPresent(attendance));
        attendance.setCheckIn(LocalTime.MIDNIGHT); assertTrue(LunchAttendancePolicy.isPresent(attendance));
        assertFalse(LunchAttendancePolicy.isPresent(null));
    }
}
