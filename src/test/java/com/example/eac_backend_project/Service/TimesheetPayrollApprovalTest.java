package com.example.eac_backend_project.Service;
import com.example.eac_backend_project.entity.attendance.*;
import com.example.eac_backend_project.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class TimesheetPayrollApprovalTest {
    TimesheetRepository sheets=mock(TimesheetRepository.class);
    AttendanceRepository attendance=mock(AttendanceRepository.class);
    TimesheetPayrollApprovalService service=new TimesheetPayrollApprovalService(sheets,attendance,mock(PayrollPeriodRepository.class));
    Timesheet t=new Timesheet();
    @BeforeEach void setup() {
        Employee e=new Employee();e.setId(1L);e.setFirstName("Demo");e.setLastName("Employee");
        Job job=new Job();job.setId(7L);job.setName("Project");
        t.setId(10L);t.setEmployee(e);t.setJob(job);t.setDate(LocalDate.of(2026,9,1));t.setRegularHours(8);t.setOvertimeHours(4);t.setAttendanceCode("P");
        when(sheets.findById(10L)).thenReturn(Optional.of(t));actor("ROLE_ADMIN");
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    void actor(String role) { SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("reviewer", "",List.of(new SimpleGrantedAuthority(role)))); }
    void approve() { service.decide(List.of(10L),"SUBMIT",null);service.decide(List.of(10L),"APPROVE",null); }
    @Test void automaticLegacyStatusDoesNotGrantPayrollApproval() { t.setStatus(TimesheetStatus.APPROVED);assertFalse(t.isPayrollApproved()); }
    @Test void requiresSubmissionAndCapturesAuthenticatedApprover() {
        assertThrows(ResponseStatusException.class,()->service.decide(List.of(10L),"APPROVE",null));
        approve();assertTrue(t.isPayrollApproved());assertEquals("reviewer",t.getPayrollApprovedBy());assertNotNull(t.getPayrollApprovedAt());
    }
    @Test void ordinaryUserCannotApproveEvenSubmittedWork() {
        service.decide(List.of(10L),"SUBMIT",null);actor("ROLE_CUSTOMER");
        ResponseStatusException error=assertThrows(ResponseStatusException.class,()->service.decide(List.of(10L),"APPROVE",null));
        assertEquals(403,error.getStatusCode().value());assertFalse(t.isPayrollApproved());
    }
    @Test void editedHoursRevokeApprovalAndClearAudit() {
        approve();t.setOvertimeHours(5);assertFalse(t.isPayrollApproved());t.invalidateChangedPayrollApproval();
        assertEquals("DRAFT",t.getPayrollApprovalStatus());assertNull(t.getPayrollApprovedBy());assertNull(t.getPayrollApprovedAt());
    }
    @Test void changedProjectDateAndEmployeeEachRequireFreshApproval() {
        approve();t.setDate(t.getDate().plusDays(1));t.invalidateChangedPayrollApproval();assertFalse(t.isPayrollApproved());
        approve();Job j=new Job();j.setId(9L);t.setJob(j);t.invalidateChangedPayrollApproval();assertFalse(t.isPayrollApproved());
        approve();Employee e=new Employee();e.setId(2L);t.setEmployee(e);t.invalidateChangedPayrollApproval();assertFalse(t.isPayrollApproved());
    }
    @Test void rejectionRequiresReasonAndCanBeResubmitted() {
        service.decide(List.of(10L),"SUBMIT",null);
        assertThrows(ResponseStatusException.class,()->service.decide(List.of(10L),"REJECT",""));
        service.decide(List.of(10L),"REJECT","Check hours");assertEquals("REJECTED",t.getPayrollApprovalStatus());
        service.decide(List.of(10L),"SUBMIT",null);assertEquals("SUBMITTED",t.getPayrollApprovalStatus());assertNull(t.getPayrollApprovedBy());
    }
    @Test void sourceDocumentNotesDoNotRevokeUnchangedWork() {
        approve();t.setNotes("Document received");t.setSourceDocumentMissing(false);t.invalidateChangedPayrollApproval();assertTrue(t.isPayrollApproved());
    }
    @Test void submittedHoursCannotChangeBeforeApproval() {
        service.decide(List.of(10L),"SUBMIT",null);t.setRegularHours(7);
        assertThrows(ResponseStatusException.class,()->service.decide(List.of(10L),"APPROVE",null));assertFalse(t.isPayrollApproved());
    }
    @Test void incomingEntityCannotStartApproved() {
        approve();t.initializePayrollApproval();assertEquals("DRAFT",t.getPayrollApprovalStatus());assertFalse(t.isPayrollApproved());
    }
    @Test void rejectsInvalidHoursBeforeSubmission() {
        t.setRegularHours(-1);
        assertThrows(ResponseStatusException.class,()->service.decide(List.of(10L),"SUBMIT",null));
        t.setRegularHours(8);t.setOvertimeHours(Double.NaN);
        assertThrows(ResponseStatusException.class,()->service.decide(List.of(10L),"SUBMIT",null));
    }
    @Test void reviewSeparatesReadyProjectOtherAttendanceAndStaleSync() {
        PayrollPeriodRepository periods=mock(PayrollPeriodRepository.class);
        TimesheetPayrollApprovalService reviewService=new TimesheetPayrollApprovalService(sheets,attendance,periods);
        var category=new com.example.eac_backend_project.entity.settings.Category();category.setName("Staff");
        t.getEmployee().setActive(true);t.getEmployee().setCategory(category);
        var period=new PayrollPeriod();period.setId(7L);period.setCategory("Staff");period.setStartDate(t.getDate());period.setEndDate(t.getDate().plusDays(4));
        when(periods.findById(7L)).thenReturn(Optional.of(period));
        approve();
        var projectAttendance=new Attendance();projectAttendance.setEmployee(t.getEmployee());projectAttendance.setDate(t.getDate());projectAttendance.setMethod("TIMESHEET_SYNC");projectAttendance.setTimesheetId(t.getId());projectAttendance.setTimesheetApprovalFingerprint(t.payrollFingerprint());projectAttendance.setTimesheetApprovedWork(projectAttendance.approvedWorkSignature());
        var otherSheet=new Timesheet();otherSheet.setId(20L);otherSheet.setEmployee(t.getEmployee());otherSheet.setDate(t.getDate().plusDays(1));
        var otherAttendance=new Attendance();otherAttendance.setEmployee(t.getEmployee());otherAttendance.setDate(otherSheet.getDate());otherAttendance.setMethod("TIMESHEET_SYNC");otherAttendance.setTimesheetId(20L);otherAttendance.setMinimumHour(8);otherAttendance.setStatus("Present");
        var stale=new Attendance();stale.setEmployee(t.getEmployee());stale.setDate(t.getDate().plusDays(2));stale.setMethod("TIMESHEET_SYNC");stale.setMinimumHour(99);stale.setStatus("Present");
        when(sheets.findByDateBetween(period.getStartDate(),period.getEndDate())).thenReturn(List.of(t,otherSheet));
        when(attendance.findByDateBetween(period.getStartDate(),period.getEndDate())).thenReturn(List.of(projectAttendance,otherAttendance,stale));
        var result=reviewService.review(7L,null,List.of());assertEquals(8.0,result.get("attendanceNormalHours"));assertEquals(1L,result.get("excludedSyncedAttendanceRecords"));
        var reviewRows=(List<Map<String,Object>>)result.get("rows");assertEquals(1,reviewRows.size());assertEquals(true,reviewRows.get(0).get("synced"));
        assertTrue(((List<?>)reviewService.review(7L,null,List.of(t.getEmployee().getId())).get("rows")).isEmpty());
        projectAttendance.setMinimumHour(4);
        var changedRows=(List<Map<String,Object>>)reviewService.review(7L,null,List.of()).get("rows");
        assertEquals(false,changedRows.get(0).get("synced"));
    }
}
