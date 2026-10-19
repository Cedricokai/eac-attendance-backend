package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.entity.attendance.Attendance;
import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.attendance.Overtime;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AttendanceReportExcelService {

    public byte[] buildAttendanceReportExcel(List<Attendance> records, LocalDate startDate, LocalDate endDate) {
        return buildAttendanceReportExcel(records, startDate, endDate, Map.of());
    }

    public byte[] buildAttendanceReportExcel(List<Attendance> records, LocalDate startDate, LocalDate endDate,
                                             Map<Long, Double> finalOvertimeByAttendanceId) {
        return buildAttendanceReportExcel(records, startDate, endDate, finalOvertimeByAttendanceId, true);
    }

    public byte[] buildAttendanceReportExcel(List<Attendance> records, LocalDate startDate, LocalDate endDate,
                                             Map<Long, Double> finalOvertimeByAttendanceId,
                                             boolean includeOvertime) {
        return buildAttendanceReportExcel(records, startDate, endDate, finalOvertimeByAttendanceId, includeOvertime, false);
    }

    /**
     * Backwards-compatible report builder. It does not have multiplier metadata,
     * so it creates synthetic overtime rows using only final hours.
     */
    public byte[] buildAttendanceReportExcel(List<Attendance> records, LocalDate startDate, LocalDate endDate,
                                             Map<Long, Double> finalOvertimeByAttendanceId,
                                             boolean includeOvertime, boolean includeStatusTotals) {
        return buildWorkbook(records, startDate, endDate, Map.of(), finalOvertimeByAttendanceId,
                includeOvertime, includeStatusTotals, false);
    }

    /**
     * Detailed builder used by general and individual emailed reports.
     * Shows raw OT, multiplier, where it was applied, final OT and explanation.
     */
    public byte[] buildAttendanceReportExcelDetailed(List<Attendance> records, LocalDate startDate, LocalDate endDate,
                                                     Map<Long, Overtime> overtimeByAttendanceId,
                                                     boolean includeOvertime,
                                                     boolean includeStatusTotals) {
        return buildWorkbook(records, startDate, endDate, overtimeByAttendanceId, Map.of(),
                includeOvertime, includeStatusTotals, true);
    }

    private byte[] buildWorkbook(List<Attendance> records,
                                 LocalDate startDate,
                                 LocalDate endDate,
                                 Map<Long, Overtime> overtimeByAttendanceId,
                                 Map<Long, Double> fallbackFinalOvertimeByAttendanceId,
                                 boolean includeOvertime,
                                 boolean includeStatusTotals,
                                 boolean detailed) {
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

            CellStyle wrapStyle = workbook.createCellStyle();
            wrapStyle.setWrapText(true);
            wrapStyle.setVerticalAlignment(VerticalAlignment.TOP);

            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Attendance Report (" + startDate + " to " + endDate + ")");
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);

            if (includeStatusTotals) {
                Row summary = sheet.createRow(1);
                String[] statuses = {"Present", "Late", "Absent", "Weekend Present"};
                for (int i = 0; i < statuses.length; i++) {
                    String status = statuses[i];
                    Cell label = summary.createCell(i * 2);
                    label.setCellValue(status + " records");
                    label.setCellStyle(headerStyle);
                    summary.createCell(i * 2 + 1).setCellValue(records.stream()
                            .filter(a -> status.equalsIgnoreCase(a.getStatus())).count());
                }
            }

            Row headerRow = sheet.createRow(2);
            String[] headers;
            if (includeOvertime && detailed) {
                headers = new String[] {
                        "Employee ID", "Employee Name", "Project", "Date", "Day", "Day Type",
                        "Check-in", "Check-out", "Hours Worked", "Normal Hours", "Standard Hours",
                        "Raw Overtime", "OT Multiplier", "Multiplier Applied To", "Final Overtime",
                        "Overtime Explanation", "Status", "Shift", "WorkType"
                };
            } else if (includeOvertime) {
                headers = new String[] {
                        "Employee ID", "Employee Name", "Project", "Date", "Check-in", "Check-out",
                        "Hours", "Standard Hours", "Overtime", "Status", "Shift", "WorkType"
                };
            } else {
                headers = new String[] {
                        "Employee ID", "Employee Name", "Project", "Date", "Check-in", "Check-out",
                        "Hours", "Standard Hours", "Status", "Shift", "WorkType"
                };
            }

            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            Map<String, double[]> totals = new LinkedHashMap<>();
            int rowIdx = 3;

            for (Attendance a : records) {
                Employee e = a.getEmployee();
                Overtime overtime = overtimeByAttendanceId.get(a.getId());
                boolean checkoutPending = a.getCheckIn() != null && a.getCheckOut() == null;
                double normalHours = checkoutPending ? 0.0 : a.getMinimumHour();
                double rawOt = checkoutPending ? 0.0 : rawOvertime(a, overtime);
                double finalOt = checkoutPending ? 0.0 : finalOvertime(a, overtime, fallbackFinalOvertimeByAttendanceId);
                double workedHours = normalHours + rawOt;

                String employeeId = e != null && e.getEmployeeId() != null ? e.getEmployeeId() : "";
                String employeeName = buildName(e);
                String jobName = e != null && e.getJob() != null && e.getJob().getName() != null
                        ? e.getJob().getName() : "Unassigned";

                Row row = sheet.createRow(rowIdx++);

                if (includeOvertime && detailed) {
                    int c = 0;
                    row.createCell(c++).setCellValue(employeeId);
                    row.createCell(c++).setCellValue(employeeName);
                    row.createCell(c++).setCellValue(jobName);

                    Cell dateCell = row.createCell(c++);
                    setDateCell(dateCell, a.getDate(), dateStyle);

                    row.createCell(c++).setCellValue(a.getDate() != null ? titleCase(a.getDate().getDayOfWeek().toString()) : "");
                    row.createCell(c++).setCellValue(determineDayType(a));
                    row.createCell(c++).setCellValue(a.getCheckIn() != null ? a.getCheckIn().toString() : "");
                    row.createCell(c++).setCellValue(a.getCheckOut() != null ? a.getCheckOut().toString() : a.getCheckIn() != null ? "Checkout pending" : "");

                    if (checkoutPending) row.createCell(c++).setCellValue("Pending");
                    else row.createCell(c++).setCellValue(workedHours);

                    row.createCell(c++).setCellValue(normalHours);
                    Cell standardCell = row.createCell(c++);
                    if (a.getStandardHours() != null) standardCell.setCellValue(a.getStandardHours());

                    row.createCell(c++).setCellValue(rawOt);
                    Cell multiplierCell = row.createCell(c++);
                    if (overtime != null && overtime.getOvertimeMultiplier() != null) {
                        multiplierCell.setCellValue(overtime.getOvertimeMultiplier());
                    }
                    row.createCell(c++).setCellValue(overtime == null || overtime.getOvertimeMultiplier() == null
                            ? "Not applied"
                            : (Boolean.TRUE.equals(overtime.getMultiplierAppliedToHours()) ? "Hours" : "Rate / Pay"));
                    row.createCell(c++).setCellValue(finalOt);
                    Cell explanationCell = row.createCell(c++);
                    explanationCell.setCellValue(buildExplanation(a, overtime, rawOt, finalOt));
                    explanationCell.setCellStyle(wrapStyle);
                    row.createCell(c++).setCellValue(a.getStatus() != null ? a.getStatus() : "");
                    row.createCell(c++).setCellValue(a.getShift() != null ? a.getShift() : "");
                    row.createCell(c).setCellValue(a.getWorkType() != null ? a.getWorkType() : "");
                } else {
                    row.createCell(0).setCellValue(employeeId);
                    row.createCell(1).setCellValue(employeeName);
                    row.createCell(2).setCellValue(jobName);
                    Cell dateCell = row.createCell(3);
                    setDateCell(dateCell, a.getDate(), dateStyle);
                    row.createCell(4).setCellValue(a.getCheckIn() != null ? a.getCheckIn().toString() : "");
                    row.createCell(5).setCellValue(a.getCheckOut() != null ? a.getCheckOut().toString() : a.getCheckIn() != null ? "Checkout pending" : "");
                    if (checkoutPending) row.createCell(6).setCellValue("Pending");
                    else row.createCell(6).setCellValue(normalHours);
                    Cell standardHoursCell = row.createCell(7);
                    if (a.getStandardHours() != null) standardHoursCell.setCellValue(a.getStandardHours());
                    int detailOffset = 0;
                    if (includeOvertime) {
                        row.createCell(8).setCellValue(finalOt);
                        detailOffset = 1;
                    }
                    row.createCell(8 + detailOffset).setCellValue(a.getStatus() != null ? a.getStatus() : "");
                    row.createCell(9 + detailOffset).setCellValue(a.getShift() != null ? a.getShift() : "");
                    row.createCell(10 + detailOffset).setCellValue(a.getWorkType() != null ? a.getWorkType() : "");
                }

                String key = employeeName + "|" + employeeId;
                double[] values = totals.computeIfAbsent(key, ignored -> new double[3]);
                values[0] += normalHours;
                values[1] += rawOt;
                values[2] += finalOt;
            }

            rowIdx++;
            Row totalsHeader = sheet.createRow(rowIdx++);
            totalsHeader.createCell(0).setCellValue("EMPLOYEE TOTALS");
            totalsHeader.getCell(0).setCellStyle(headerStyle);

            if (includeOvertime && detailed) {
                Row th = sheet.createRow(rowIdx++);
                String[] totalHeaders = {"Employee", "Normal Hours", "Raw Overtime", "Final Overtime"};
                for (int i = 0; i < totalHeaders.length; i++) {
                    th.createCell(i).setCellValue(totalHeaders[i]);
                    th.getCell(i).setCellStyle(headerStyle);
                }
                for (Map.Entry<String, double[]> entry : totals.entrySet()) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(entry.getKey().split("\\|", 2)[0]);
                    row.createCell(1).setCellValue(entry.getValue()[0]);
                    row.createCell(2).setCellValue(entry.getValue()[1]);
                    row.createCell(3).setCellValue(entry.getValue()[2]);
                }
                Row grand = sheet.createRow(rowIdx);
                grand.createCell(0).setCellValue("GRAND TOTAL");
                grand.getCell(0).setCellStyle(headerStyle);
                grand.createCell(1).setCellValue(totals.values().stream().mapToDouble(v -> v[0]).sum());
                grand.createCell(2).setCellValue(totals.values().stream().mapToDouble(v -> v[1]).sum());
                grand.createCell(3).setCellValue(totals.values().stream().mapToDouble(v -> v[2]).sum());
            } else {
                for (Map.Entry<String, double[]> entry : totals.entrySet()) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(entry.getKey().split("\\|", 2)[0]);
                    row.createCell(7).setCellValue(entry.getValue()[0]);
                    if (includeOvertime) row.createCell(8).setCellValue(entry.getValue()[2]);
                }
                Row grandTotal = sheet.createRow(rowIdx);
                grandTotal.createCell(0).setCellValue("GRAND TOTAL");
                grandTotal.getCell(0).setCellStyle(headerStyle);
                grandTotal.createCell(7).setCellValue(totals.values().stream().mapToDouble(v -> v[0]).sum());
                if (includeOvertime) grandTotal.createCell(8).setCellValue(totals.values().stream().mapToDouble(v -> v[2]).sum());
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                int width = sheet.getColumnWidth(i);
                if (width > 18000) sheet.setColumnWidth(i, 18000);
            }
            if (includeOvertime && detailed) {
                sheet.setColumnWidth(15, 22000);
            }

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                workbook.write(out);
                return out.toByteArray();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to build attendance report Excel", e);
        }
    }

    private void setDateCell(Cell dateCell, LocalDate date, CellStyle dateStyle) {
        if (date != null) {
            dateCell.setCellValue(java.sql.Date.valueOf(date));
            dateCell.setCellStyle(dateStyle);
        } else {
            dateCell.setCellValue("");
        }
    }

    private double rawOvertime(Attendance a, Overtime overtime) {
        if (overtime != null && overtime.getRawOvertimeHours() != null) return overtime.getRawOvertimeHours();
        return a.getOvertime() != null ? a.getOvertime() : 0.0;
    }

    private double finalOvertime(Attendance a, Overtime overtime, Map<Long, Double> fallback) {
        if (overtime != null && overtime.getOvertimeHours() != null) return overtime.getOvertimeHours();
        return fallback.getOrDefault(a.getId(), a.getOvertime() != null ? a.getOvertime() : 0.0);
    }

    private String determineDayType(Attendance a) {
        String status = a.getStatus() == null ? "" : a.getStatus().toLowerCase(Locale.ROOT);
        if (status.contains("holiday")) return "Holiday";
        if (status.contains("weekend") || status.contains("off day")) return "Weekend / Off Day";
        if (a.getDate() != null && (a.getDate().getDayOfWeek() == DayOfWeek.SATURDAY || a.getDate().getDayOfWeek() == DayOfWeek.SUNDAY)) {
            return "Weekend";
        }
        return "Regular Work Day";
    }

    private String buildExplanation(Attendance a, Overtime overtime, double rawOt, double finalOt) {
        if (rawOt <= 0) return "No overtime generated.";
        if ("Holiday Present".equalsIgnoreCase(a.getStatus())) return "Holiday overtime equals worked hours multiplied by the holiday multiplier, minus normal hours; no additional multiplier is applied.";
        boolean weekend = determineDayType(a).toLowerCase(Locale.ROOT).contains("weekend");
        StringBuilder text = new StringBuilder();
        if (weekend && a.getMinimumHour() <= 0.0001) {
            text.append("Worked on configured weekend/off-day; all worked hours were treated as overtime.");
        } else {
            text.append("Overtime generated from hours outside normal hours.");
        }
        if (overtime != null && overtime.getOvertimeMultiplier() != null) {
            text.append(" Multiplier ").append(trimNumber(overtime.getOvertimeMultiplier())).append("x applied to ")
                    .append(Boolean.TRUE.equals(overtime.getMultiplierAppliedToHours()) ? "hours" : "rate/pay").append(".");
        }
        if (Math.abs(finalOt - rawOt) > 0.0001) {
            text.append(" Raw OT ").append(trimNumber(rawOt)).append(" became ").append(trimNumber(finalOt)).append(" final OT hours.");
        }
        return text.toString();
    }

    private String buildName(Employee e) {
        if (e == null) return "Unknown";
        return ((e.getFirstName() == null ? "" : e.getFirstName()) + " " +
                (e.getLastName() == null ? "" : e.getLastName())).trim();
    }

    private String titleCase(String value) {
        if (value == null || value.isBlank()) return "";
        String lower = value.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private String trimNumber(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.000001) return String.valueOf((long) Math.rint(value));
        return String.format(Locale.ENGLISH, "%.2f", value);
    }
}
