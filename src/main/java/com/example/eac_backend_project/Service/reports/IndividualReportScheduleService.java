package com.example.eac_backend_project.Service.reports;

import com.example.eac_backend_project.Service.AttendanceReportExcelService;
import com.example.eac_backend_project.Service.email.EmailService;
import com.example.eac_backend_project.entity.settings.IndividualReportSchedule;
import com.example.eac_backend_project.repository.settings.IndividualReportScheduleRepository;
import com.example.eac_backend_project.repository.IndividualReportAttendanceRepository;
import com.example.eac_backend_project.repository.EmployeeRepository;
import com.example.eac_backend_project.repository.OvertimeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class IndividualReportScheduleService {
    private static final Logger log = LoggerFactory.getLogger(IndividualReportScheduleService.class);
    public static final ZoneId ZONE = ZoneId.of("Africa/Accra");
    public static final String CONTACT = "If you have any questions or concerns about your attendance report, please contact the Human Resource Manager at dianna.mensah@eaconcept.org or call 0595092422.";
    public record Settings(boolean enabled, String frequency, Integer weekday, String sendTime,
                           List<Long> employeeIds, boolean includeOvertime, List<Long> excludeOvertimeEmployeeIds,
                           String message) {}
    public record Status(Settings settings, Instant nextRun, Instant lastAttempt, String lastResult) {}
    public record Period(LocalDate start, LocalDate end) {}
    private final IndividualReportScheduleRepository schedules;
    private final EmployeeRepository employees;
    private final IndividualReportAttendanceRepository attendance;
    private final OvertimeRepository overtime;
    private final AttendanceReportExcelService excel;
    private final EmailService email;
    private final ObjectMapper mapper;
    private final com.example.eac_backend_project.Service.ApprovedLeaveAttendanceService approvedLeave;

    public IndividualReportScheduleService(IndividualReportScheduleRepository schedules, EmployeeRepository employees,
            IndividualReportAttendanceRepository attendance, OvertimeRepository overtime, AttendanceReportExcelService excel,
            EmailService email, ObjectMapper mapper, com.example.eac_backend_project.Service.ApprovedLeaveAttendanceService approvedLeave) {
        this.schedules = schedules; this.employees = employees; this.attendance = attendance;
        this.overtime = overtime; this.excel = excel; this.email = email; this.mapper = mapper; this.approvedLeave = approvedLeave;
    }
    public Status status() {
        return schedules.findById(1L).map(this::statusOf).orElse(new Status(
            new Settings(false, "WEEKLY", 1, "08:00", List.of(), true, List.of(), "Please find your attendance report attached."), null, null, null));
    }
    private Settings decode(IndividualReportSchedule s) {
        try { return mapper.readValue(s.getConfiguration(), Settings.class); }
        catch (Exception e) { throw new IllegalStateException("Cannot read individual report schedule", e); }
    }
    private Status statusOf(IndividualReportSchedule s) {
        return new Status(decode(s), s.getNextRun(), s.getLastAttempt(), s.getLastResult());
    }
    public Status save(Settings input) {
        validate(input);
        var ids = input.employeeIds().stream().distinct().toList();
        if (input.enabled() && employees.findAllById(ids).size() != ids.size()) throw invalid("Some selected employees no longer exist. Refresh the employee list.");
        var settings = new Settings(input.enabled(), input.frequency(), input.weekday(), input.sendTime(), ids,
            input.includeOvertime(), input.excludeOvertimeEmployeeIds() == null ? List.of() : input.excludeOvertimeEmployeeIds().stream().filter(ids::contains).distinct().toList(),
            input.message() == null ? "" : input.message().trim());
        var stored = schedules.findById(1L).orElseGet(IndividualReportSchedule::new);
        try { stored.setConfiguration(mapper.writeValueAsString(settings)); }
        catch (Exception e) { throw new IllegalStateException(e); }
        stored.setEnabled(settings.enabled());
        stored.setNextRun(settings.enabled() ? nextRun(settings, Instant.now()) : null);
        return statusOf(schedules.saveAndFlush(stored));
    }
    public static void validate(Settings s) {
        if (s == null || !("DAILY".equals(s.frequency()) || "WEEKLY".equals(s.frequency()))) throw invalid("Choose daily or weekly delivery.");
        if (s.weekday() == null || s.weekday() < 1 || s.weekday() > 7) throw invalid("Choose a valid weekday.");
        try {
            if (s.sendTime() == null || !s.sendTime().matches("\\d{2}:\\d{2}")) throw new IllegalArgumentException();
            LocalTime.parse(s.sendTime());
        } catch (Exception e) { throw invalid("Choose a valid send time."); }
        if (s.employeeIds() == null || s.employeeIds().isEmpty() || s.employeeIds().stream().anyMatch(id -> id == null || id <= 0)) throw invalid("Select at least one employee.");
        if (s.message() != null && s.message().length() > 5000) throw invalid("The message must be 5,000 characters or fewer.");
    }
    private static ResponseStatusException invalid(String reason) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason); }
    public static Instant nextRun(Settings s, Instant after) {
        var local = after.atZone(ZONE);
        var day = local.toLocalDate();
        if ("WEEKLY".equals(s.frequency())) day = day.with(TemporalAdjusters.nextOrSame(DayOfWeek.of(s.weekday())));
        var candidate = day.atTime(LocalTime.parse(s.sendTime())).atZone(ZONE);
        if (!candidate.toInstant().isAfter(after)) candidate = "DAILY".equals(s.frequency()) ? candidate.plusDays(1) : candidate.plusWeeks(1);
        return candidate.toInstant();
    }
    public static Period period(Settings s, LocalDate runDate) {
        if ("DAILY".equals(s.frequency())) return new Period(runDate.minusDays(1), runDate.minusDays(1));
        var start = runDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1);
        return new Period(start, start.plusDays(6));
    }
    @Scheduled(fixedDelay = 60000)
    public void tick() {
        var stored = schedules.findById(1L).orElse(null);
        var now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        if (stored == null || !stored.isEnabled() || stored.getNextRun() == null || stored.getNextRun().isAfter(now)) return;
        var settings = decode(stored);
        // The database claim prevents duplicate batches across polling, restarts and multiple app instances.
        if (schedules.claim(stored.getVersion(), now, nextRun(settings, now)) != 1) return;
        try {
            schedules.recordResult(now, deliver(settings, period(settings, stored.getNextRun().atZone(ZONE).toLocalDate())));
        } catch (Exception e) {
            log.error("Individual attendance report schedule failed", e);
            schedules.recordResult(now, "Failed. Check server logs; this batch will not be retried automatically.");
        }
    }
    String deliver(Settings s, Period p) {
        var records = approvedLeave.includeApprovedLeave(attendance.findAllForReportBetweenDates(p.start(), p.end()), p.start(), p.end());
        Map<Long, Double> finalOt = new HashMap<>();
        // Explicit zero prevents the renderer from falling back to unapproved attendance overtime.
        for (var record : records) finalOt.put(record.getId(), 0.0);
        for (var ot : overtime.findByDateBetween(p.start(), p.end())) {
            if ("Approved".equalsIgnoreCase(ot.getStatus()) && ot.getAttendance() != null) {
                finalOt.merge(ot.getAttendance().getId(), ot.getOvertimeHours() == null ? 0.0 : ot.getOvertimeHours(), Double::sum);
            }
        }
        int sent = 0, skipped = 0, failed = 0;
        for (Long id : s.employeeIds()) {
            var employee = employees.findById(id).orElse(null);
            if (employee == null || employee.getEmail() == null || !employee.getEmail().trim().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) { skipped++; continue; }
            try {
                var individual = records.stream().filter(a -> a.getEmployee() != null && id.equals(a.getEmployee().getId())).toList();
                boolean includeOt = s.includeOvertime() && !s.excludeOvertimeEmployeeIds().contains(id);
                byte[] attachment = excel.buildAttendanceReportExcel(individual, p.start(), p.end(), finalOt, includeOt);
                String body = "<p>Attendance report: " + p.start() + " to " + p.end() + "</p><p>" + escape(s.message()) + "</p><p>" + CONTACT + "</p>";
                email.sendEmailWithExcelAttachment(employee.getEmail().trim(), "Your attendance report: " + p.start() + " to " + p.end(), body,
                    "attendance-report-" + p.start() + "-to-" + p.end() + ".xlsx", attachment);
                sent++;
            } catch (Exception e) { failed++; log.error("Scheduled attendance report failed for employee {}", id, e); }
        }
        return sent + " sent, " + skipped + " skipped (missing employee/email), " + failed + " failed";
    }
    private static String escape(String text) {
        return (text == null ? "" : text).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;").replace("\n", "<br/>");
    }
}
