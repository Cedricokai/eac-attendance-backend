package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.Service.AttendanceReportExcelService;
import com.example.eac_backend_project.Service.email.EmailService;
import com.example.eac_backend_project.dto.AttendanceReportEmailRequest;
import com.example.eac_backend_project.entity.attendance.Attendance;
import com.example.eac_backend_project.repository.AttendanceRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
public class AttendanceReportsController {

    private final AttendanceRepository attendanceRepository;
    private final AttendanceReportExcelService excelService;
    private final EmailService emailService;

    public AttendanceReportsController(AttendanceRepository attendanceRepository,
                                       AttendanceReportExcelService excelService,
                                       EmailService emailService) {
        this.attendanceRepository = attendanceRepository;
        this.excelService = excelService;
        this.emailService = emailService;
    }

    @PostMapping("/attendance/email")
    public ResponseEntity<?> emailAttendanceReport(@Valid @RequestBody AttendanceReportEmailRequest request) {
        LocalDate start = request.getStartDate();
        LocalDate end = request.getEndDate();

        List<String> recipients = normalizeEmails(request.getToEmails());
        if (recipients.isEmpty()) {
            return ResponseEntity.badRequest().body("Recipient email list is empty");
        }

        List<Attendance> records = attendanceRepository.findAllForReportBetweenDates(start, end);

        byte[] excelBytes = excelService.buildAttendanceReportExcel(records, start, end);

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

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
