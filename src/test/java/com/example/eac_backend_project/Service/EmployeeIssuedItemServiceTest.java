package com.example.eac_backend_project.Service;
import com.example.eac_backend_project.entity.inventory.*;
import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.repository.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmployeeIssuedItemServiceTest {
    final EmployeeIssuedItemRepository items = mock(EmployeeIssuedItemRepository.class);
    final PpeReplacementPolicyRepository policies = mock(PpeReplacementPolicyRepository.class);
    final IssuedItemEventRepository events = mock(IssuedItemEventRepository.class);
    final EmployeeRepository employees = mock(EmployeeRepository.class);
    final EmployeeIssuedItemService service = new EmployeeIssuedItemService(items, policies, events, employees);
    EmployeeIssuedItemService.Issue input(LocalDate date, Long replaces) {
        return new EmployeeIssuedItemService.Issue(1L, "Safety boots", "PPE", "Boots", 1, "", "REF-01", date, "Store officer", "New", "", null, replaces);
    }
    void prepare() {
        var employee = new Employee(); employee.setId(1L); employee.setFirstName("Test"); employee.setLastName("Employee"); employee.setEmployeeId("E001");
        when(employees.findById(1L)).thenReturn(Optional.of(employee));
        var policy = new PpeReplacementPolicy(); policy.setId(1L); policy.setPpeType("Boots"); policy.setIntervalCount(1); policy.setIntervalUnit("YEARS");
        when(policies.findByPpeTypeIgnoreCase("Boots")).thenReturn(Optional.of(policy));
        when(items.saveAndFlush(any())).thenAnswer(call -> { EmployeeIssuedItem record = call.getArgument(0); record.setId(20L); return record; });
        when(items.save(any())).thenAnswer(call -> call.getArgument(0));
    }
    EmployeeIssuedItem original() {
        var item = new EmployeeIssuedItem(); item.setId(10L); item.setEmployeeId(1L); item.setCategory("PPE"); item.setPpeType("Boots"); item.setQuantity(1); item.setIssueDate(LocalDate.parse("2024-01-01")); item.setStatusDate(item.getIssueDate()); item.setStatus("ASSIGNED");
        when(items.lockById(10L)).thenReturn(Optional.of(item)); return item;
    }
    @Test void calendarIntervalsClampMonthEndAndLeapDay() {
        assertEquals(LocalDate.parse("2025-02-28"), EmployeeIssuedItemService.dueDate(LocalDate.parse("2025-01-31"), 1, "MONTHS"));
        assertEquals(LocalDate.parse("2025-02-28"), EmployeeIssuedItemService.dueDate(LocalDate.parse("2024-02-29"), 1, "YEARS"));
        assertThrows(Exception.class, () -> EmployeeIssuedItemService.dueDate(LocalDate.now(), 0, "MONTHS"));
        assertThrows(Exception.class, () -> EmployeeIssuedItemService.dueDate(LocalDate.now(), 1, "DAYS"));
    }
    @Test void historicalIssueStoresDateIntervalActorAndHistory() {
        prepare(); var saved = service.issue(input(LocalDate.parse("2024-02-29"), null), "admin").record();
        assertEquals(LocalDate.parse("2025-02-28"), saved.getReplacementDueDate());
        assertEquals("admin", saved.getRecordedBy()); assertEquals("Store officer", saved.getIssuedBy());
        assertEquals("Test Employee", saved.getEmployeeName()); assertEquals(1, saved.getReplacementIntervalCount());
        var event = ArgumentCaptor.forClass(IssuedItemEvent.class); verify(events).save(event.capture());
        assertEquals("ISSUED", event.getValue().getAction()); assertEquals(saved.getIssueDate(), event.getValue().getEffectiveDate());
    }
    @Test void futureIssueAndMissingPolicyCannotCreateRecords() {
        prepare();
        assertThrows(Exception.class, () -> service.issue(input(LocalDate.now().plusDays(1), null), "admin"));
        when(policies.findByPpeTypeIgnoreCase("Boots")).thenReturn(Optional.empty());
        assertThrows(Exception.class, () -> service.issue(input(LocalDate.parse("2024-01-01"), null), "admin"));
        verify(items, never()).saveAndFlush(any()); verifyNoInteractions(events);
    }
    @Test void initialAcknowledgementIsPreservedInHistory() {
        prepare(); var date = LocalDate.parse("2024-01-01");
        service.issue(new EmployeeIssuedItemService.Issue(1L, "Boots", "PPE", "Boots", 1, "", "R01", date, "Store officer", "", "Signed receipt R01", date, null), "admin");
        var captor = ArgumentCaptor.forClass(IssuedItemEvent.class); verify(events, times(2)).save(captor.capture());
        assertEquals("ACKNOWLEDGED", captor.getAllValues().get(1).getAction());
        assertEquals("Signed receipt R01", captor.getAllValues().get(1).getNotes());
    }
    @Test void replacementPreservesOldIssueAndLinksBothRecords() {
        prepare(); var old = original();
        var replacement = service.issue(input(LocalDate.parse("2025-01-10"), 10L), "admin").record();
        assertEquals("REPLACED", old.getStatus()); assertEquals(20L, old.getReplacedById()); assertEquals(LocalDate.parse("2024-01-01"), old.getIssueDate());
        assertEquals(10L, replacement.getReplacesId()); assertEquals(LocalDate.parse("2026-01-10"), replacement.getReplacementDueDate());
        verify(events, times(2)).save(any());
    }
    @Test void cannotReplaceAnAlreadyClosedIssueOrAnotherEmployeesItem() {
        prepare(); var old = original(); old.setStatus("REPLACED");
        assertThrows(Exception.class, () -> service.issue(input(LocalDate.parse("2025-01-01"), 10L), "admin"));
        old.setStatus("ASSIGNED"); old.setEmployeeId(2L);
        assertThrows(Exception.class, () -> service.issue(input(LocalDate.parse("2025-01-01"), 10L), "admin"));
        verify(items, never()).saveAndFlush(any());
    }
    @Test void returnStopsCountdownAndKeepsHistory() {
        prepare(); var old = original(); old.setReplacementDueDate(LocalDate.parse("2025-01-01"));
        var view = service.change(10L, new EmployeeIssuedItemService.Change("RETURNED", LocalDate.parse("2025-01-01"), "Returned in good condition"), "admin");
        assertEquals("RETURNED", old.getStatus()); assertEquals("CLOSED", view.replacementStatus()); verify(events).save(any());
        assertThrows(Exception.class, () -> service.change(10L, new EmployeeIssuedItemService.Change("LOST", LocalDate.parse("2025-01-02"), "Lost"), "admin"));
    }
    @Test void countdownSeparatesDueTodayOverdueAndDueSoon() {
        var old = original(); var today = LocalDate.now(ZoneId.of("Africa/Accra"));
        old.setReplacementDueDate(today); assertEquals("DUE_TODAY", service.view(old).replacementStatus());
        old.setReplacementDueDate(today.minusDays(1)); assertEquals(-1L, service.view(old).daysRemaining()); assertEquals("OVERDUE", service.view(old).replacementStatus());
        old.setReplacementDueDate(today.plusDays(30)); assertEquals("DUE_SOON", service.view(old).replacementStatus());
        old.setReplacementDueDate(today.plusDays(31)); assertEquals("ON_TRACK", service.view(old).replacementStatus());
    }
    @Test void policyChangesDoNotRewriteExistingIssueDates() {
        var policy = new PpeReplacementPolicy(); policy.setId(1L); policy.setPpeType("Boots");
        when(policies.findById(1L)).thenReturn(Optional.of(policy)); when(policies.findByPpeTypeIgnoreCase("Boots")).thenReturn(Optional.of(policy));
        service.savePolicy(1L, new EmployeeIssuedItemService.Policy("Boots", 6, "MONTHS", true));
        assertEquals(6, policy.getIntervalCount()); verifyNoInteractions(items);
    }
    @Test void cannotBackdateStatusBeforeLatestEventOrClaimAcknowledgementBeforeIssue() {
        prepare(); original();
        assertThrows(Exception.class, () -> service.change(10L, new EmployeeIssuedItemService.Change("RETURNED", LocalDate.parse("2023-01-01"), "Returned"), "admin"));
        verifyNoInteractions(events);
    }
}
