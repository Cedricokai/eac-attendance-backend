package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.Exceptions.AttendanceRequestException;
import com.example.eac_backend_project.dto.AttendanceRequestCreateDTO;
import com.example.eac_backend_project.entity.attendance.*;
import com.example.eac_backend_project.entity.inventory.Role;
import com.example.eac_backend_project.entity.inventory.User;
import com.example.eac_backend_project.repository.AttendanceRepository;
import com.example.eac_backend_project.repository.AttendanceRequestRepository;
import com.example.eac_backend_project.repository.EmployeeRepository;
import com.example.eac_backend_project.Service.email.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceRequestServiceTest {
    @Mock AttendanceRequestRepository requests;
    @Mock AttendanceRepository attendances;
    @Mock EmployeeRepository employees;
    @Mock UserService users;
    @Mock EmailService emailService;

    private AttendanceRequestService service;
    private Employee employee;
    private User employeeUser;
    private User hrUser;

    @BeforeEach
    void setUp() {
        service = new AttendanceRequestService(requests, attendances, employees, users, emailService);
        employee = employee(10L);
        employeeUser = user(20L, "ROLE_CUSTOMER", employee);
        hrUser = user(30L, "ROLE_HR", null);
    }

    @Test
    void submissionCreatesPendingRequestWithoutAttendance() {
        when(users.getCurrentUser()).thenReturn(employeeUser);
        when(employees.findById(10L)).thenReturn(Optional.of(employee));
        when(requests.save(any())).thenAnswer(call -> { AttendanceRequest value = call.getArgument(0); value.setId(1L); return value; });

        var result = service.create(new AttendanceRequestCreateDTO(10L, LocalDate.now(),
                LocalTime.of(8, 0), LocalTime.of(17, 0), "Day", "Device missed entry", "LOG-10"));

        assertEquals("Pending", result.status());
        assertNull(result.attendanceId());
        verify(attendances, never()).save(any());
        verify(emailService).sendAttendanceRequestReviewNotification(anyList(), eq("ROLE_CUSTOMER user"));
    }

    @Test
    void verificationDoesNotCreateAttendanceUntilExplicitlyAdded() {
        AttendanceRequest request = pendingRequest();
        when(users.getCurrentUser()).thenReturn(hrUser);
        when(requests.findAllByIdForUpdate(any())).thenReturn(List.of(request));
        when(requests.saveAll(any())).thenAnswer(call -> call.getArgument(0));

        var result = service.verify(List.of(1L));

        assertEquals("Verified", result.get(0).status());
        assertNull(result.get(0).attendanceId());
        verify(attendances, never()).save(any());
    }

    @Test
    void verifiedRequestCanBeAddedThenRemovedFromAttendance() {
        AttendanceRequest request = pendingRequest();
        request.setStatus(AttendanceRequestStatus.VERIFIED);
        when(users.getCurrentUser()).thenReturn(hrUser);
        when(requests.findAllByIdForUpdate(any())).thenReturn(List.of(request));
        when(attendances.save(any())).thenAnswer(call -> { Attendance value = call.getArgument(0); value.setId(99L); return value; });
        when(requests.saveAll(any())).thenAnswer(call -> call.getArgument(0));

        var added = service.addToAttendance(List.of(1L));
        assertEquals(99L, added.get(0).attendanceId());

        var removed = service.removeFromAttendance(List.of(1L));
        assertNull(removed.get(0).attendanceId());
        assertEquals("Verified", removed.get(0).status());
        verify(attendances).delete(any(Attendance.class));
    }

    @Test
    void rejectionCreatesNoAttendance() {
        AttendanceRequest request = pendingRequest();
        when(users.getCurrentUser()).thenReturn(hrUser);
        when(requests.findAllByIdForUpdate(any())).thenReturn(List.of(request));
        when(requests.saveAll(any())).thenAnswer(call -> call.getArgument(0));

        var result = service.reject(List.of(1L), "Insufficient evidence");

        assertEquals("Rejected", result.get(0).status());
        verify(attendances, never()).save(any());
    }

    @Test
    void processedRequestCannotBeVerifiedAgain() {
        AttendanceRequest request = pendingRequest();
        request.setStatus(AttendanceRequestStatus.VERIFIED);
        when(users.getCurrentUser()).thenReturn(hrUser);
        when(requests.findAllByIdForUpdate(any())).thenReturn(List.of(request));

        AttendanceRequestException error = assertThrows(AttendanceRequestException.class,
                () -> service.verify(List.of(1L)));

        assertEquals(409, error.getStatus().value());
        verify(attendances, never()).save(any());
    }

    private AttendanceRequest pendingRequest() {
        AttendanceRequest value = new AttendanceRequest();
        value.setId(1L);
        value.setEmployee(employee);
        value.setDate(LocalDate.now());
        value.setCheckIn(LocalTime.of(8, 0));
        value.setCheckOut(LocalTime.of(17, 0));
        value.setShift("Day");
        value.setReason("Device missed entry");
        value.setStatus(AttendanceRequestStatus.PENDING);
        value.setRequestedBy(employeeUser);
        value.setRequestedAt(java.time.LocalDateTime.now());
        return value;
    }

    private Employee employee(Long id) {
        Employee value = new Employee();
        value.setId(id);
        value.setEmployeeId("EAC010");
        value.setFirstName("Ama");
        value.setLastName("Mensah");
        value.setActive(true);
        return value;
    }

    private User user(Long id, String roleName, Employee linkedEmployee) {
        Role role = new Role(); role.setName(roleName);
        User value = new User();
        value.setId(id); value.setName(roleName + " user"); value.setUserName(roleName.toLowerCase());
        value.setEmail(roleName.toLowerCase() + "@example.com"); value.setRole(role); value.setEmployee(linkedEmployee);
        return value;
    }
}
