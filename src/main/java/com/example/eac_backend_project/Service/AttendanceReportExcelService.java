package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.entity.attendance.Attendance;
import com.example.eac_backend_project.entity.attendance.Employee;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;

@Service
public class AttendanceReportExcelService {

    public byte[] buildAttendanceReportExcel(List<Attendance> records, LocalDate startDate, LocalDate endDate) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Attendance Report");

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setWrapText(true);

            CellStyle dateStyle = workbook.createCellStyle();
            CreationHelper creationHelper = workbook.getCreationHelper();
            dateStyle.setDataFormat(creationHelper.createDataFormat().getFormat("yyyy-mm-dd"));

            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Attendance Report (" + startDate + " to " + endDate + ")");
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);

            Row headerRow = sheet.createRow(2);
            String[] headers = new String[] {
                    "Employee ID",
                    "Employee Name",
                    "Date",
                    "Check-in",
                    "Check-out",
                    "Hours",
                    "Overtime",
                    "Status",
                    "Shift",
                    "WorkType"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            int rowIdx = 3;
            for (Attendance a : records) {
                Employee e = a.getEmployee();

                Row row = sheet.createRow(rowIdx++);

                String employeeId = e != null && e.getEmployeeId() != null ? e.getEmployeeId() : "";
                String employeeName = buildName(e);

                row.createCell(0).setCellValue(employeeId);
                row.createCell(1).setCellValue(employeeName);

                Cell dateCell = row.createCell(2);
                if (a.getDate() != null) {
                    dateCell.setCellValue(java.sql.Date.valueOf(a.getDate()));
                    dateCell.setCellStyle(dateStyle);
                } else {
                    dateCell.setCellValue("");
                }

                row.createCell(3).setCellValue(a.getCheckIn() != null ? a.getCheckIn().toString() : "");
                row.createCell(4).setCellValue(a.getCheckOut() != null ? a.getCheckOut().toString() : "");

                row.createCell(5).setCellValue(a.getMinimumHour());
                row.createCell(6).setCellValue(a.getOvertime() != null ? a.getOvertime() : 0.0);

                row.createCell(7).setCellValue(a.getStatus() != null ? a.getStatus() : "");
                row.createCell(8).setCellValue(a.getShift() != null ? a.getShift() : "");
                row.createCell(9).setCellValue(a.getWorkType() != null ? a.getWorkType() : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                workbook.write(out);
                return out.toByteArray();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate attendance report Excel: " + e.getMessage(), e);
        }
    }

    private String buildName(Employee e) {
        if (e == null) return "";
        String first = e.getFirstName() != null ? e.getFirstName() : "";
        String last = e.getLastName() != null ? e.getLastName() : "";
        return (first + " " + last).trim();
    }
}
