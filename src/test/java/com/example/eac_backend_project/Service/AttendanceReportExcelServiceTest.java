package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.entity.attendance.Attendance;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class AttendanceReportExcelServiceTest {
    @Test void standardHoursArePresentWithAndWithoutOvertime() throws Exception {
        Attendance a = new Attendance();
        a.setId(1L); a.setMinimumHour(7); a.setStandardHours(8.0);
        a.setStatus("Present"); a.setShift("Day"); a.setWorkType("Regular");
        Attendance missing = new Attendance(); missing.setId(2L);
        for (boolean overtime : new boolean[]{true, false}) {
            byte[] bytes = new AttendanceReportExcelService().buildAttendanceReportExcel(
                    List.of(a, missing), LocalDate.of(2026,9,1), LocalDate.of(2026,9,10), Map.of(1L,2.0), overtime);
            try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
                var sheet = workbook.getSheetAt(0);
                assertEquals("Standard Hours", sheet.getRow(2).getCell(7).getStringCellValue());
                assertEquals(7, sheet.getRow(3).getCell(6).getNumericCellValue());
                assertEquals(8, sheet.getRow(3).getCell(7).getNumericCellValue());
                assertEquals(CellType.BLANK, sheet.getRow(4).getCell(7).getCellType());
                assertEquals("Present", sheet.getRow(3).getCell(overtime ? 9 : 8).getStringCellValue());
                assertEquals("Regular", sheet.getRow(3).getCell(overtime ? 11 : 10).getStringCellValue());
                if (overtime) {
                    assertEquals(2, sheet.getRow(3).getCell(8).getNumericCellValue());
                    assertEquals(2, sheet.getRow(sheet.getLastRowNum()).getCell(8).getNumericCellValue());
                }
            }
        }
    }

    @Test void incompleteCheckInShowsPendingWithoutInventingWorkedHours() throws Exception {
        var a=new Attendance();a.setId(1L);a.setStatus("Present");a.setCheckIn(java.time.LocalTime.of(8,0));
        a.setMinimumHour(7);a.setStandardHours(8.0);
        byte[] bytes=new AttendanceReportExcelService().buildAttendanceReportExcel(List.of(a),LocalDate.of(2026,9,1),LocalDate.of(2026,9,1));
        try(var workbook=new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var sheet=workbook.getSheetAt(0);var row=sheet.getRow(3);
            assertEquals("Checkout pending",row.getCell(5).getStringCellValue());
            assertEquals("Pending",row.getCell(6).getStringCellValue());
            assertEquals(8,row.getCell(7).getNumericCellValue());
            assertEquals("Present",row.getCell(9).getStringCellValue());
            assertEquals(0,sheet.getRow(sheet.getLastRowNum()).getCell(6).getNumericCellValue());
        }
    }

    @Test void manualReportShowsStatusTotalsForIncludedRecords() throws Exception {
        var records=new java.util.ArrayList<Attendance>();
        long id=1;
        for (String status : java.util.Arrays.asList("Present","present","Late","Absent","Half Day",null)) {
            var a=new Attendance();a.setId(id++);a.setStatus(status);records.add(a);
        }
        for(boolean overtime : new boolean[]{true,false}) {
            byte[] bytes=new AttendanceReportExcelService().buildAttendanceReportExcel(records,LocalDate.of(2026,9,1),LocalDate.of(2026,9,2),Map.of(),overtime,true);
            try(var workbook=new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
                var sheet=workbook.getSheetAt(0);var totals=sheet.getRow(1);
                assertEquals("Present records",totals.getCell(0).getStringCellValue());
                assertEquals(2,totals.getCell(1).getNumericCellValue());
                assertEquals("Late records",totals.getCell(2).getStringCellValue());
                assertEquals(1,totals.getCell(3).getNumericCellValue());
                assertEquals("Absent records",totals.getCell(4).getStringCellValue());
                assertEquals(1,totals.getCell(5).getNumericCellValue());
                assertEquals("Employee ID",sheet.getRow(2).getCell(0).getStringCellValue());
            }
        }
        byte[] empty=new AttendanceReportExcelService().buildAttendanceReportExcel(List.of(),LocalDate.of(2026,9,1),LocalDate.of(2026,9,1),Map.of(),true,true);
        try(var workbook=new XSSFWorkbook(new ByteArrayInputStream(empty))) {
            for(int column : new int[]{1,3,5}) assertEquals(0,workbook.getSheetAt(0).getRow(1).getCell(column).getNumericCellValue());
        }
    }
}
