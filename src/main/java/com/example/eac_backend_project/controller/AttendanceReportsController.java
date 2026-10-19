package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.Service.AttendanceReportExcelService;
import com.example.eac_backend_project.Service.email.EmailService;
import com.example.eac_backend_project.dto.AttendanceReportEmailRequest;
import com.example.eac_backend_project.entity.attendance.Attendance;
import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.attendance.Overtime;
import com.example.eac_backend_project.repository.AttendanceRepository;
import com.example.eac_backend_project.repository.OvertimeRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
public class AttendanceReportsController {

    private final AttendanceRepository attendanceRepository;
    private final AttendanceReportExcelService excelService;
    private final EmailService emailService;
    private final OvertimeRepository overtimeRepository;
    private final com.example.eac_backend_project.Service.ApprovedLeaveAttendanceService approvedLeave;

    public AttendanceReportsController(AttendanceRepository attendanceRepository,
                                       AttendanceReportExcelService excelService,
                                       EmailService emailService,
                                       OvertimeRepository overtimeRepository, com.example.eac_backend_project.Service.ApprovedLeaveAttendanceService approvedLeave) {
        this.attendanceRepository = attendanceRepository;
        this.excelService = excelService;
        this.emailService = emailService;
        this.overtimeRepository = overtimeRepository;
        this.approvedLeave = approvedLeave;
    }

    @PostMapping("/attendance/email")
    public ResponseEntity<?> emailAttendanceReport(@Valid @RequestBody AttendanceReportEmailRequest request) {
        LocalDate start = request.getStartDate();
        LocalDate end = request.getEndDate();

        if (start == null || end == null) {
            return ResponseEntity.badRequest().body("Start date and end date are required");
        }
        if (end.isBefore(start)) {
            return ResponseEntity.badRequest().body("End date must be on or after start date");
        }

        List<String> recipients = normalizeEmails(request.getToEmails());
        if (recipients.isEmpty()) {
            return ResponseEntity.badRequest().body("Recipient email list is empty");
        }

        List<Attendance> records = filterRecords(
                approvedLeave.includeApprovedLeave(attendanceRepository.findAllForReportBetweenDates(start, end), start, end),
                request.getJobId(), request.getEmployeeIds());

        Map<Long, Overtime> overtimeByAttendanceId = getOvertimeRecordsByAttendanceId(start, end);
        applyExcludedOvertimeEmployees(overtimeByAttendanceId, records, request.getExcludeOvertimeEmployeeIds());

        boolean includeOvertime = !Boolean.FALSE.equals(request.getIncludeOvertime());
        byte[] excelBytes = excelService.buildAttendanceReportExcelDetailed(
                records, start, end, overtimeByAttendanceId, includeOvertime, true);

        String subject = (request.getSubject() == null || request.getSubject().trim().isEmpty())
                ? defaultSubject(start, end)
                : request.getSubject().trim();

        String htmlBody = buildSimpleHtmlBody(request.getMessage(), start, end, records.size());
        String fileName = "attendance-report-" + start + "-to-" + end + ".xlsx";

        for (String to : recipients) {
            emailService.sendEmailWithExcelAttachment(to, subject, htmlBody, fileName, excelBytes);
        }

        return ResponseEntity.ok("Report emailed to " + recipients.size() + " recipient(s)");
    }

    /**
     * Sends the employee-specific attendance report used by reports.jsx.
     * The attachment includes weekend/off-day overtime treatment, raw OT,
     * multiplier, multiplier target and final OT.
     */
    @PostMapping("/attendance/employee/email")
    public ResponseEntity<?> emailEmployeeAttendanceReport(@RequestBody Map<String, Object> request) {
        Long employeeId = asLong(request.get("employeeId"));
        LocalDate start = asDate(request.get("startDate"));
        LocalDate end = asDate(request.get("endDate"));

        if (employeeId == null) {
            return ResponseEntity.badRequest().body("Employee is required");
        }
        if (start == null || end == null) {
            return ResponseEntity.badRequest().body("Start date and end date are required");
        }
        if (end.isBefore(start)) {
            return ResponseEntity.badRequest().body("End date must be on or after start date");
        }

        List<Attendance> records = approvedLeave.includeApprovedLeave(attendanceRepository.findAllForReportBetweenDates(start, end), start, end)
                .stream()
                .filter(a -> a.getEmployee() != null && employeeId.equals(a.getEmployee().getId()))
                .sorted(Comparator.comparing(Attendance::getDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        if (records.isEmpty()) {
            return ResponseEntity.badRequest().body("No attendance records found for this employee in the selected period");
        }

        Employee employee = records.get(0).getEmployee();
        String requestEmail = request.get("employeeEmail") == null ? "" : String.valueOf(request.get("employeeEmail")).trim();
        String recipient = !requestEmail.isBlank()
                ? requestEmail
                : (employee != null && employee.getEmail() != null ? employee.getEmail().trim() : "");

        if (recipient.isBlank()) {
            return ResponseEntity.badRequest().body("The selected employee does not have an email address");
        }

        Map<Long, Overtime> overtimeByAttendanceId = getOvertimeRecordsByAttendanceId(start, end);
        byte[] excelBytes = excelService.buildAttendanceReportExcelDetailed(
                records, start, end, overtimeByAttendanceId, true, true);

        String employeeName = employee == null ? "Employee" : buildEmployeeName(employee);
        String requestedSubject = request.get("subject") == null ? "" : String.valueOf(request.get("subject")).trim();
        String subject = requestedSubject.isBlank()
                ? "Attendance Report for " + employeeName + " (" + start + " to " + end + ")"
                : requestedSubject;

        String message = request.get("message") == null ? "" : String.valueOf(request.get("message"));
        String htmlBody = buildEmployeeHtmlBody(message, employeeName, start, end, records, overtimeByAttendanceId);
        String safeEmployee = employeeName.replaceAll("[^A-Za-z0-9._-]+", "_");
        String fileName = "attendance-report-" + safeEmployee + "-" + start + "-to-" + end + ".xlsx";

        emailService.sendEmailWithExcelAttachment(recipient, subject, htmlBody, fileName, excelBytes);
        return ResponseEntity.ok("Employee attendance report emailed to " + recipient);
    }

    // Existing attendance callers retain their operational (all-hours) view.
    public ResponseEntity<?> previewAttendanceReport(LocalDate startDate, LocalDate endDate, Long jobId,
            List<Long> employeeIds, List<Long> excludeOvertimeEmployeeIds) {
        return previewAttendanceReport(startDate, endDate, jobId, employeeIds, excludeOvertimeEmployeeIds, false);
    }

    @GetMapping("/attendance/preview")
    public ResponseEntity<?> previewAttendanceReport(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam(required = false) Long jobId,
            @RequestParam(required = false) List<Long> employeeIds,
            @RequestParam(required = false) List<Long> excludeOvertimeEmployeeIds,
            @RequestParam(defaultValue = "false") boolean approvedOnly) {
        if (endDate.isBefore(startDate)) {
            return ResponseEntity.badRequest().body("End date must be on or after start date");
        }

        List<Attendance> attendance = approvedLeave.includeApprovedLeave(
                attendanceRepository.findAllForReportBetweenDates(startDate, endDate), startDate, endDate);
        var approved = approvedOnly ? com.example.eac_backend_project.Service.reports.ApprovedOvertimePreview.build(
                attendance, overtimeRepository.findApprovedOvertimeForPayrollPeriod(startDate, endDate)) : null;
        List<Attendance> records = filterRecords(approvedOnly ? approved.records() : attendance, jobId, employeeIds);
        Map<Long, Overtime> overtimeByAttendanceId = approvedOnly ? approved.overtimeByAttendanceId()
                : getOvertimeRecordsByAttendanceId(startDate, endDate);
        Set<Long> excluded = excludeOvertimeEmployeeIds == null
                ? Set.of() : Set.copyOf(excludeOvertimeEmployeeIds);

        List<Map<String, Object>> rows = records.stream().map(a -> {
            Map<String, Object> row = new LinkedHashMap<>();
            Employee employee = a.getEmployee();
            Overtime overtime = overtimeByAttendanceId.get(a.getId());
            boolean excludeOvertime = employee != null && excluded.contains(employee.getId());

            boolean noApprovedOvertime = approvedOnly && overtime == null;
            double rawOvertime = excludeOvertime || noApprovedOvertime ? 0.0 : rawOvertimeHours(a, overtime);
            double finalOvertime = excludeOvertime || noApprovedOvertime ? 0.0 : finalOvertimeHours(a, overtime);
            Double multiplier = excludeOvertime ? null : overtimeMultiplier(overtime);
            boolean multiplierOnHours = !excludeOvertime && overtime != null
                    && Boolean.TRUE.equals(overtime.getMultiplierAppliedToHours());

            row.put("attendanceId", a.getId());
            row.put("employeeId", employee != null ? employee.getId() : null);
            row.put("employeeNumber", employee != null ? employee.getEmployeeId() : "");
            row.put("employeeName", employee != null ? buildEmployeeName(employee) : "");
            row.put("jobName", employee != null && employee.getJob() != null ? employee.getJob().getName() : "Unassigned");
            row.put("date", a.getDate());
            row.put("dayName", a.getDate() != null ? a.getDate().getDayOfWeek().toString() : "");
            row.put("dayType", determineDayType(a));
            row.put("checkIn", a.getCheckIn());
            row.put("checkOut", a.getCheckOut());
            row.put("status", a.getStatus());
            row.put("normalHours", a.getMinimumHour());
            row.put("hoursWorked", a.getMinimumHour() + rawOvertime);
            row.put("rawOvertimeHours", rawOvertime);
            row.put("overtimeHours", finalOvertime);
            row.put("overtimeMultiplier", multiplier);
            row.put("multiplierAppliedToHours", multiplierOnHours);
            boolean weekendAllOvertime = isWeekendAllHoursOvertime(a, rawOvertime);
            row.put("multiplierApplication", multiplier == null
                    ? "Not applied"
                    : (weekendAllOvertime ? "Overtime Hours (Weekend Rule)" : (multiplierOnHours ? "Hours" : "Rate / Pay")));
            row.put("overtimeExplanation", noApprovedOvertime ? "No approved overtime included." : buildOvertimeExplanation(a, overtime, rawOvertime, finalOvertime));
            row.put("overtimeApprovalStatus", overtime == null ? "None" : Objects.toString(overtime.getStatus(), ""));
            return row;
        }).toList();

        Map<Long, Map<String, Object>> totals = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Long employeeId = (Long) row.get("employeeId");
            Map<String, Object> total = totals.computeIfAbsent(employeeId, ignored -> {
                Map<String, Object> value = new LinkedHashMap<>();
                value.put("employeeId", employeeId);
                value.put("employeeNumber", row.get("employeeNumber"));
                value.put("employeeName", row.get("employeeName"));
                value.put("jobName", row.get("jobName"));
                value.put("totalNormalHours", 0.0);
                value.put("totalRawOvertimeHours", 0.0);
                value.put("totalOvertimeHours", 0.0);
                return value;
            });
            total.put("totalNormalHours", (Double) total.get("totalNormalHours") + ((Number) row.get("normalHours")).doubleValue());
            total.put("totalRawOvertimeHours", (Double) total.get("totalRawOvertimeHours") + ((Number) row.get("rawOvertimeHours")).doubleValue());
            total.put("totalOvertimeHours", (Double) total.get("totalOvertimeHours") + ((Number) row.get("overtimeHours")).doubleValue());
        }

        double totalNormalHours = totals.values().stream()
                .mapToDouble(value -> ((Number) value.get("totalNormalHours")).doubleValue()).sum();
        double totalRawOvertimeHours = totals.values().stream()
                .mapToDouble(value -> ((Number) value.get("totalRawOvertimeHours")).doubleValue()).sum();
        double totalOvertimeHours = totals.values().stream()
                .mapToDouble(value -> ((Number) value.get("totalOvertimeHours")).doubleValue()).sum();

        return ResponseEntity.ok(Map.of(
                "rows", rows,
                "totals", totals.values(),
                "recordCount", rows.size(),
                "approvedOnly", approvedOnly,
                "totalNormalHours", totalNormalHours,
                "totalRawOvertimeHours", totalRawOvertimeHours,
                "totalOvertimeHours", totalOvertimeHours));
    }

    private List<Attendance> filterRecords(List<Attendance> records, Long jobId, List<Long> employeeIds) {
        Set<Long> selected = employeeIds == null ? Set.of() : Set.copyOf(employeeIds);
        return records.stream()
                .filter(a -> jobId == null || (a.getEmployee() != null && a.getEmployee().getJob() != null
                        && jobId.equals(a.getEmployee().getJob().getId())))
                .filter(a -> selected.isEmpty() || (a.getEmployee() != null && selected.contains(a.getEmployee().getId())))
                .toList();
    }

    private Map<Long, Overtime> getOvertimeRecordsByAttendanceId(LocalDate start, LocalDate end) {
        Map<Long, Overtime> result = new HashMap<>();
        for (Overtime overtime : overtimeRepository.findByDateBetween(start, end)) {
            if (overtime.getAttendance() != null && overtime.getAttendance().getId() != null) {
                result.put(overtime.getAttendance().getId(), overtime);
            }
        }
        return result;
    }

    private void applyExcludedOvertimeEmployees(Map<Long, Overtime> overtimeByAttendanceId,
                                                List<Attendance> records,
                                                List<Long> excludeOvertimeEmployeeIds) {
        if (excludeOvertimeEmployeeIds == null || excludeOvertimeEmployeeIds.isEmpty()) return;
        Set<Long> excluded = Set.copyOf(excludeOvertimeEmployeeIds);
        for (Attendance attendance : records) {
            if (attendance.getEmployee() != null && excluded.contains(attendance.getEmployee().getId())) {
                Overtime excludedOvertime = new Overtime();
                excludedOvertime.setRawOvertimeHours(0.0);
                excludedOvertime.setOvertimeHours(0.0);
                excludedOvertime.setOvertimeMultiplier(null);
                excludedOvertime.setMultiplierAppliedToHours(false);
                overtimeByAttendanceId.put(attendance.getId(), excludedOvertime);
            }
        }
    }

    private double rawOvertimeHours(Attendance attendance, Overtime overtime) {
        if (overtime != null && overtime.getRawOvertimeHours() != null) {
            return overtime.getRawOvertimeHours();
        }
        return attendance.getOvertime() != null ? attendance.getOvertime() : 0.0;
    }

    private double finalOvertimeHours(Attendance attendance, Overtime overtime) {
        if (overtime != null && overtime.getOvertimeHours() != null) {
            return overtime.getOvertimeHours();
        }
        return attendance.getOvertime() != null ? attendance.getOvertime() : 0.0;
    }

    private Double overtimeMultiplier(Overtime overtime) {
        return overtime != null ? overtime.getOvertimeMultiplier() : null;
    }

    private String determineDayType(Attendance attendance) {
        String status = attendance.getStatus() == null ? "" : attendance.getStatus().toLowerCase(Locale.ROOT);
        if (status.contains("holiday")) return "Holiday";
        if (status.contains("weekend") || status.contains("off day")) return "Weekend / Off Day";
        if (attendance.getDate() != null) {
            DayOfWeek day = attendance.getDate().getDayOfWeek();
            if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) return "Weekend";
        }
        return "Regular Work Day";
    }

    private boolean isWeekendAllHoursOvertime(Attendance attendance, double rawOvertime) {
        if (attendance == null || rawOvertime <= 0) return false;
        String dayType = determineDayType(attendance);
        return attendance.getMinimumHour() <= 0.0001
                && ("Weekend / Off Day".equals(dayType) || "Weekend".equals(dayType));
    }

    private String buildOvertimeExplanation(Attendance attendance, Overtime overtime,
                                            double rawOvertime, double finalOvertime) {
        if (rawOvertime <= 0) {
            return "No overtime generated.";
        }

        if ("Holiday Present".equalsIgnoreCase(attendance.getStatus())) return "Holiday overtime equals worked hours multiplied by the holiday multiplier, minus normal hours; no additional multiplier is applied.";

        boolean weekendOrOffDay = "Weekend / Off Day".equals(determineDayType(attendance))
                || "Weekend".equals(determineDayType(attendance));
        StringBuilder text = new StringBuilder();

        if (weekendOrOffDay && attendance.getMinimumHour() <= 0.0001) {
            text.append("Worked on configured weekend/off-day; normal hours are 0 and all worked hours are raw overtime.");
        } else {
            text.append("Overtime generated from hours outside normal hours.");
        }

        if (overtime != null && overtime.getOvertimeMultiplier() != null) {
            text.append(" Multiplier ")
                    .append(trimNumber(overtime.getOvertimeMultiplier()))
                    .append("x applied to ")
                    .append(weekendOrOffDay && attendance.getMinimumHour() <= 0.0001
                            ? "the weekend overtime hours"
                            : (Boolean.TRUE.equals(overtime.getMultiplierAppliedToHours()) ? "hours" : "rate/pay"))
                    .append(".");
        }

        if (Math.abs(finalOvertime - rawOvertime) > 0.0001) {
            text.append(" Raw OT ").append(trimNumber(rawOvertime))
                    .append(" became ").append(trimNumber(finalOvertime)).append(" final OT hours.");
        }
        return text.toString();
    }

    private String buildEmployeeHtmlBody(String message, String employeeName,
                                         LocalDate start, LocalDate end,
                                         List<Attendance> records,
                                         Map<Long, Overtime> overtimeByAttendanceId) {
        double totalNormal = records.stream().mapToDouble(Attendance::getMinimumHour).sum();
        double totalRawOt = records.stream().mapToDouble(a -> rawOvertimeHours(a, overtimeByAttendanceId.get(a.getId()))).sum();
        double totalFinalOt = records.stream().mapToDouble(a -> finalOvertimeHours(a, overtimeByAttendanceId.get(a.getId()))).sum();
        long weekendWorked = records.stream().filter(a -> rawOvertimeHours(a, overtimeByAttendanceId.get(a.getId())) > 0
                && determineDayType(a).toLowerCase(Locale.ROOT).contains("weekend")).count();

        String msg = (message == null || message.trim().isEmpty())
                ? "Please find attached your attendance report."
                : escapeHtml(message.trim()).replace("\n", "<br/>");

        return "<div style=\"font-family:Arial,sans-serif;color:#111827;\">" +
                "<h3 style=\"margin:0 0 8px 0;\">Individual Attendance Report</h3>" +
                "<div>Employee: <strong>" + escapeHtml(employeeName) + "</strong></div>" +
                "<div>Period: <strong>" + start + "</strong> to <strong>" + end + "</strong></div>" +
                "<div style=\"margin-top:10px;\">Normal hours: <strong>" + trimNumber(totalNormal) + "</strong></div>" +
                "<div>Raw overtime hours: <strong>" + trimNumber(totalRawOt) + "</strong></div>" +
                "<div>Final overtime hours: <strong>" + trimNumber(totalFinalOt) + "</strong></div>" +
                "<div>Weekend/off-day records with overtime: <strong>" + weekendWorked + "</strong></div>" +
                "<div style=\"margin-top:12px;\">" + msg + "</div>" +
                "<div style=\"margin-top:12px;font-size:12px;color:#4b5563;\">The attachment shows raw overtime, the stored multiplier, whether the multiplier was applied to hours or rate/pay, final overtime, and the reason for each weekend/off-day treatment.</div>" +
                "</div>";
    }

    private List<String> normalizeEmails(List<String> emails) {
        if (emails == null) return List.of();
        return emails.stream()
                .filter(e -> e != null && !e.trim().isEmpty())
                .flatMap(e -> List.of(e.split(",")).stream())
                .map(String::trim)
                .filter(e -> !e.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    private String defaultSubject(LocalDate start, LocalDate end) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
        return "Attendance Report (" + start.format(fmt) + " - " + end.format(fmt) + ")";
    }

    private String buildSimpleHtmlBody(String message, LocalDate start, LocalDate end, int rows) {
        String msg = (message == null || message.trim().isEmpty())
                ? "Please find attached the attendance report."
                : escapeHtml(message.trim()).replace("\n", "<br/>");

        return "<div style=\"font-family: Arial, sans-serif; color:#111827;\">" +
                "<h3 style=\"margin:0 0 8px 0;\">Attendance Report</h3>" +
                "<div style=\"margin:0 0 10px 0;\">Period: <strong>" + start + "</strong> to <strong>" + end + "</strong></div>" +
                "<div style=\"margin:0 0 10px 0;\">Rows: <strong>" + rows + "</strong></div>" +
                "<div style=\"margin-top:12px;\">" + msg + "</div>" +
                "</div>";
    }

    private Long asLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        try { return Long.valueOf(String.valueOf(value)); }
        catch (Exception ignored) { return null; }
    }

    private LocalDate asDate(Object value) {
        if (value == null) return null;
        try { return LocalDate.parse(String.valueOf(value)); }
        catch (Exception ignored) { return null; }
    }

    private String buildEmployeeName(Employee employee) {
        return ((employee.getFirstName() == null ? "" : employee.getFirstName()) + " " +
                (employee.getLastName() == null ? "" : employee.getLastName())).trim();
    }

    private String trimNumber(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.000001) return String.valueOf((long) Math.rint(value));
        return String.format(Locale.ENGLISH, "%.2f", value);
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
