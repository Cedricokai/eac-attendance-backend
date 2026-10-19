package com.example.eac_backend_project.Service;
import com.example.eac_backend_project.entity.attendance.*;
import com.example.eac_backend_project.entity.settings.Category;
import com.example.eac_backend_project.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
@DataJpaTest(properties={"spring.jpa.hibernate.ddl-auto=create-drop","spring.datasource.url=jdbc:h2:mem:approval-test;MODE=MySQL;DB_CLOSE_DELAY=-1","spring.jpa.show-sql=false","spring.flyway.enabled=false","spring.liquibase.enabled=false","spring.jpa.database-platform=org.hibernate.dialect.H2Dialect","spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"})
class TimesheetPayrollEligibilityQueryTest {
    @Autowired EntityManager em;
    @Autowired AttendanceRepository attendance;
    @Autowired OvertimeRepository overtime;
    @Test void payrollQueriesExcludePendingStaleAndDeletedTimesheetsButIncludeIndependentAttendance() {
        Category c=new Category();c.setName("Demo category");em.persist(c);
        Employee e=new Employee();e.setEmployeeId("DEMO-APPROVAL");e.setFirstName("Demo");e.setLastName("Worker");e.setActive(true);e.setCategory(c);em.persist(e);
        Job job=new Job();job.setName("Project");job.setClient("Demo client");job.setStartDate(LocalDate.of(2026,8,1));job.setBillingCycle(Job.BillingCycle.MONTHLY);em.persist(job);
        LocalDate day=LocalDate.of(2026,9,1);
        Timesheet t=new Timesheet();t.setEmployee(e);t.setJob(job);t.setDate(day);t.setRegularHours(8);t.setOvertimeHours(4);t.setAttendanceCode("P");em.persist(t);em.flush();
        Attendance a=new Attendance();a.setEmployee(e);a.setDate(day);a.setMethod("TIMESHEET_SYNC");a.setTimesheetId(t.getId());a.setMinimumHour(8);a.setStatus("Present");a.setTimesheetApprovalFingerprint(t.payrollFingerprint());a.setTimesheetApprovedWork(a.approvedWorkSignature());em.persist(a);
        Overtime ot=new Overtime();ot.setEmployee(e);ot.setAttendance(a);ot.setDate(day);ot.setStatus("Approved");ot.setOvertimeHours(4.0);ot.setCalculatedOvertimePay(400.0);ot.setMultiplierAppliedToHours(true);em.persist(ot);em.flush();
        assertTrue(attendance.findDailyHoursByPeriod(day,day).isEmpty());assertTrue(overtime.findApprovedOvertimeForPayrollPeriod(day,day).isEmpty());
        t.setPayrollApprovalStatus("APPROVED");t.setPayrollApprovedBy("reviewer");t.setPayrollApprovedAt(LocalDateTime.now());t.setPayrollReviewFingerprint(t.payrollFingerprint());em.flush();
        assertEquals(1,attendance.findDailyHoursByPeriod(day,day).size());assertEquals(1,attendance.findAttendanceDatesByPeriod(day,day).size());assertEquals(1,attendance.findValidAttendanceForPayroll(day,day).size());
        assertEquals(1,overtime.findApprovedOvertimeForPayrollPeriod(day,day).size());assertEquals(1,overtime.summarizeApprovedOvertimeBetweenDatesForCategory(day,day,"Demo category").size());
        t.setRegularHours(7);em.flush();assertEquals("DRAFT",t.getPayrollApprovalStatus());assertTrue(attendance.findDailyHoursByPeriod(day,day).isEmpty());
        t.setPayrollApprovalStatus("APPROVED");t.setPayrollApprovedBy("reviewer");t.setPayrollApprovedAt(LocalDateTime.now());t.setPayrollReviewFingerprint(t.payrollFingerprint());em.flush();
        assertTrue(attendance.findDailyHoursByPeriod(day,day).isEmpty()); // old sync version still excluded
        a.setTimesheetApprovalFingerprint(t.payrollFingerprint());a.setMinimumHour(7);a.setTimesheetApprovedWork(a.approvedWorkSignature());em.flush();assertEquals(1,attendance.findDailyHoursByPeriod(day,day).size());
        a.setMinimumHour(99);em.flush();assertNull(a.getTimesheetApprovalFingerprint());assertTrue(attendance.findDailyHoursByPeriod(day,day).isEmpty());
        em.remove(t);em.flush();assertTrue(attendance.findDailyHoursByPeriod(day,day).isEmpty());assertTrue(overtime.findApprovedOvertimeForPayrollPeriod(day,day).isEmpty());
        Attendance direct=new Attendance();direct.setEmployee(e);direct.setDate(day.plusDays(1));direct.setMinimumHour(8);direct.setStatus("Present");em.persist(direct);em.flush();
        assertEquals(1,attendance.findDailyHoursByPeriod(day,day.plusDays(1)).size());
    }
}
