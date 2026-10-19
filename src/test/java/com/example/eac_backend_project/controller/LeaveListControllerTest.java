package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.inventory.User;
import com.example.eac_backend_project.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class LeaveListControllerTest {
    final LeaveRepository leaves = mock(LeaveRepository.class);
    final EmployeeRepository employees = mock(EmployeeRepository.class);
    final UserRepository users = mock(UserRepository.class);
    final LeaveListController controller = new LeaveListController(leaves, employees, users);
    UsernamePasswordAuthenticationToken auth(String role) {
        return new UsernamePasswordAuthenticationToken("person", "", List.of(new SimpleGrantedAuthority(role)));
    }
    @Test void administratorsAndHrReadTheRegisterWithoutEmployeeLookups() {
        for (String role : List.of("ROLE_ADMIN", "ROLE_HR", "ROLE_HUMAN_RESOURCES")) {
            assertEquals(200, controller.list(auth(role)).getStatusCode().value());
        }
        verify(leaves, times(3)).findListRows(null);
        verifyNoInteractions(users, employees);
    }
    @Test void otherRolesOnlyReadTheirOwnEmployeeRequests() {
        User user = new User(); user.setEmail("person@example.com");
        Employee employee = new Employee(); employee.setId(42L);
        when(users.findByUserName("person")).thenReturn(Optional.of(user));
        when(employees.findByEmail("person@example.com")).thenReturn(Optional.of(employee));
        controller.list(auth("ROLE_SUPERVISOR"));
        verify(leaves).findListRows(42L);
        verify(leaves, never()).findListRows(null);
    }
    @Test void missingEmployeeNeverFallsBackToAllRequests() {
        User user = new User(); user.setEmail("person@example.com");
        when(users.findByUserName("person")).thenReturn(Optional.of(user));
        when(employees.findByEmail("person@example.com")).thenReturn(Optional.empty());
        assertEquals(List.of(), controller.list(auth("ROLE_USER")).getBody());
        verifyNoInteractions(leaves);
    }
    @Test void missingUserNeverFallsBackToAllRequests() {
        assertEquals(List.of(), controller.list(auth("ROLE_USER")).getBody());
        verifyNoInteractions(leaves, employees);
    }
    @Test void unauthenticatedRequestsAreRejected() {
        assertEquals(401, controller.list(null).getStatusCode().value());
        assertEquals(401, controller.list(new UsernamePasswordAuthenticationToken("person", "")).getStatusCode().value());
        verifyNoInteractions(leaves, employees, users);
    }
}
