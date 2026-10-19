package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.entity.attendance.*;
import com.example.eac_backend_project.entity.settings.Category;
import com.example.eac_backend_project.repository.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import java.io.ByteArrayInputStream;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class ApprovedLeaveAttendanceServiceTest {
    final AttendanceRepository attendance = mock(AttendanceRepository.class);
    final LeaveRepository leaves = mock(LeaveRepository.class);
    final OvertimeRepository overtime = mock(OvertimeRepository.class);
    final ApprovedLeaveAttendanceService service = new ApprovedLeaveAttendanceService(attendance, leaves, overtime);
    final LocalDate monday = LocalDate.of(2026,9,21);
    Leave leave() {
        var employee = new Employee(); employee.setId(1L); employee.setCategory(new Category());
        var leave = new Leave(); leave.setId(5L); leave.setEmployee(employee); leave.setStartDate(monday);
        leave.setEndDate(monday); leave.setStatus("Approved"); return leave;
    }
    Attendance row(Leave leave, String status) {
        var row = new Attendance(); row.setId(10L); row.setEmployee(leave.getEmployee());
        row.setDate(monday); row.setStatus(status); return row;
    }
    @Test void approvedLeaveReplacesAbsentInPlaceAndRepeatedApprovalDoesNotDuplicate() {
        var leave = leave(); var absent = row(leave,"Absent");
        when(attendance.findByEmployeeIdAndDateBetween(1L,monday,monday)).thenReturn(List.of(absent));
        service.synchronize(leave); service.synchronize(leave);
        assertEquals(10L,absent.getId()); assertEquals("On Leave",absent.getStatus());
        assertEquals(8,absent.getMinimumHour()); assertEquals(0,absent.getOvertime());
        assertNull(absent.getCheckIn()); assertNull(absent.getCheckOut());
        verify(attendance,times(2)).saveAll(argThat(rows -> rows.iterator().next() == absent));
    }
    @Test void missingDaysAreCreatedButPendingLeaveDoesNothing() {
        var leave=leave(); leave.setStatus("Pending"); service.synchronize(leave);
        verifyNoInteractions(attendance);
        leave.setStatus("Approved"); service.synchronize(leave);
        verify(attendance).saveAll(argThat(rows -> "On Leave".equals(rows.iterator().next().getStatus())));
    }
    @Test void workedAttendanceIsPreserved() {
        var leave=leave(); var worked=row(leave,"Present"); worked.setMinimumHour(6);
        when(attendance.findByEmployeeIdAndDateBetween(1L,monday,monday)).thenReturn(List.of(worked));
        service.synchronize(leave); verify(attendance,never()).saveAll(any());
        assertEquals(6,worked.getMinimumHour());
    }
    @Test void reportIncludesOldApprovalsAndReplacesAbsentWithoutMutatingStoredRow() throws Exception {
        var leave=leave(); var absent=row(leave,"Absent");
        when(leaves.findApprovedForAttendanceReport(monday,monday)).thenReturn(List.of(leave,leave));
        var result=service.includeApprovedLeave(List.of(absent),monday,monday);
        assertEquals(1,result.size()); assertEquals("On Leave",result.get(0).getStatus());
        assertEquals(8,result.get(0).getMinimumHour()); assertEquals("Absent",absent.getStatus());
        assertTrue(result.get(0).getId()<0); verifyNoInteractions(attendance);
        byte[] bytes=new AttendanceReportExcelService().buildAttendanceReportExcel(result,monday,monday,Map.of(),false);
        try(var workbook=new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var sheet=workbook.getSheetAt(0);
            assertEquals(8,sheet.getRow(3).getCell(6).getNumericCellValue());
            assertEquals("On Leave",sheet.getRow(3).getCell(8).getStringCellValue());
        }
    }
    @Test void reportRespectsWeekendPolicyAndClipsLeaveToRequestedPeriod() {
        var leave=leave(); leave.setStartDate(monday.minusDays(2)); leave.setEndDate(monday.plusDays(1));
        when(leaves.findApprovedForAttendanceReport(monday.minusDays(1),monday)).thenReturn(List.of(leave));
        assertEquals(1,service.includeApprovedLeave(List.of(),monday.minusDays(1),monday).size());
        leave.getEmployee().getCategory().setExcludeWeekendsFromLeave(false);
        assertEquals(2,service.includeApprovedLeave(List.of(),monday.minusDays(1),monday).size());
    }
}
