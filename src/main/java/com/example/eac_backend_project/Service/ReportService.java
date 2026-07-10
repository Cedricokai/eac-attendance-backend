package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.attendance.Job;
import com.example.eac_backend_project.entity.attendance.JobAttachment;
import com.example.eac_backend_project.entity.attendance.Timesheet;
import com.example.eac_backend_project.repository.EmployeeRepository;
import com.example.eac_backend_project.repository.JobAttachmentRepository;
import com.example.eac_backend_project.repository.JobRepository;
import com.example.eac_backend_project.repository.TimesheetRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.GrayColor;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ReportService {

    private final TimesheetRepository timesheetRepository;
    private final EmployeeRepository employeeRepository;
    private final JobRepository jobRepository;
    private final JobAttachmentRepository jobAttachmentRepository;
    private final FileStorageService fileStorageService;

    // Regex patterns for date extraction from filenames
    private static final Pattern DATE_PATTERN_ISO = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})");
    private static final Pattern DATE_PATTERN_COMPACT = Pattern.compile("(\\d{4})(\\d{2})(\\d{2})");
    private static final Pattern DATE_PATTERN_MDY = Pattern.compile("(\\d{1,2})[-/](\\d{1,2})[-/](\\d{4})");
    private static final Pattern DATE_PATTERN_MONTH = Pattern.compile("([A-Za-z]+)[-_. ]?(\\d{1,2})[-_. ]?(\\d{4})", Pattern.CASE_INSENSITIVE);

    public ReportService(TimesheetRepository timesheetRepository,
                         EmployeeRepository employeeRepository,
                         JobRepository jobRepository,
                         JobAttachmentRepository jobAttachmentRepository,
                         FileStorageService fileStorageService) {
        this.timesheetRepository = timesheetRepository;
        this.employeeRepository = employeeRepository;
        this.jobRepository = jobRepository;
        this.jobAttachmentRepository = jobAttachmentRepository;
        this.fileStorageService = fileStorageService;
    }

    // ==================== INDIVIDUAL REPORT WITH ATTACHMENTS ====================

    public byte[] generateIndividualReportWithAttachments(Long employeeId, LocalDate startDate, LocalDate endDate) throws Exception {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));

        List<Timesheet> timesheets = timesheetRepository.findByEmployeeIdAndDateBetween(employeeId, startDate, endDate);

        // Get attachments for this employee's job
        Job employeeJob = employee.getJob();
        List<JobAttachment> allAttachments = new ArrayList<>();
        if (employeeJob != null && employeeJob.getId() != null) {
            allAttachments = jobAttachmentRepository.findByJobId(employeeJob.getId());
        }

        // Filter attachments by date range (parse date from filename)
        Map<LocalDate, List<JobAttachment>> attachmentsByDate = filterAttachmentsByDateRange(allAttachments, startDate, endDate);

        // Generate PDF with attachment summary
        byte[] pdfBytes = generateIndividualReportPdfWithAttachments(employee, timesheets, startDate, endDate, attachmentsByDate);

        // Create ZIP with PDF and matching attachments
        return createZipPackage(pdfBytes, attachmentsByDate,
                "Individual_Report_" + employee.getFirstName() + "_" + employee.getLastName() + "_" + startDate + "_to_" + endDate);
    }

    // ==================== GENERAL REPORT WITH ATTACHMENTS ====================

    public byte[] generateGeneralReportWithAttachments(LocalDate startDate, LocalDate endDate, Long jobId) throws Exception {
        List<Timesheet> timesheets;
        String jobName = null;
        List<JobAttachment> allAttachments = new ArrayList<>();

        if (jobId != null) {
            Job job = jobRepository.findById(jobId).orElse(null);
            jobName = job != null ? job.getName() : "Unknown Job";
            timesheets = timesheetRepository.findByJobId(jobId);
            allAttachments = jobAttachmentRepository.findByJobId(jobId);
        } else {
            timesheets = timesheetRepository.findByDateBetween(startDate, endDate);
        }

        if (jobId != null) {
            timesheets = timesheets.stream()
                    .filter(ts -> !ts.getDate().isBefore(startDate) && !ts.getDate().isAfter(endDate))
                    .collect(Collectors.toList());
        }

        // Filter attachments by date range
        Map<LocalDate, List<JobAttachment>> attachmentsByDate = filterAttachmentsByDateRange(allAttachments, startDate, endDate);

        // Generate PDF with attachment summary
        byte[] pdfBytes = generateGeneralReportPdfWithAttachments(timesheets, startDate, endDate, jobId, jobName, attachmentsByDate);

        // Create ZIP with PDF and matching attachments
        return createZipPackage(pdfBytes, attachmentsByDate,
                "General_Report_" + (jobName != null ? jobName : "All_Jobs") + "_" + startDate + "_to_" + endDate);
    }

    // ==================== HELPER: Extract Date from Filename ====================

    private LocalDate extractDateFromFilename(String filename) {
        if (filename == null || filename.isEmpty()) return null;

        String name = filename.toLowerCase();

        // Try ISO format: 2026-04-03
        Matcher isoMatcher = DATE_PATTERN_ISO.matcher(name);
        if (isoMatcher.find()) {
            try {
                return LocalDate.of(
                        Integer.parseInt(isoMatcher.group(1)),
                        Integer.parseInt(isoMatcher.group(2)),
                        Integer.parseInt(isoMatcher.group(3))
                );
            } catch (Exception e) { /* continue */ }
        }

        // Try compact format: 20260403
        Matcher compactMatcher = DATE_PATTERN_COMPACT.matcher(name);
        if (compactMatcher.find()) {
            try {
                return LocalDate.of(
                        Integer.parseInt(compactMatcher.group(1)),
                        Integer.parseInt(compactMatcher.group(2)),
                        Integer.parseInt(compactMatcher.group(3))
                );
            } catch (Exception e) { /* continue */ }
        }

        // Try MDY format: 04/03/2026 or 04-03-2026
        Matcher mdyMatcher = DATE_PATTERN_MDY.matcher(name);
        if (mdyMatcher.find()) {
            try {
                int month = Integer.parseInt(mdyMatcher.group(1));
                int day = Integer.parseInt(mdyMatcher.group(2));
                int year = Integer.parseInt(mdyMatcher.group(3));
                if (month >= 1 && month <= 12 && day >= 1 && day <= 31) {
                    return LocalDate.of(year, month, day);
                }
            } catch (Exception e) { /* continue */ }
        }

        // Try month name format: april_3_2026
        Matcher monthMatcher = DATE_PATTERN_MONTH.matcher(name);
        if (monthMatcher.find()) {
            try {
                String monthStr = monthMatcher.group(1);
                int day = Integer.parseInt(monthMatcher.group(2));
                int year = Integer.parseInt(monthMatcher.group(3));

                Map<String, Integer> monthMap = new HashMap<>();
                monthMap.put("january", 1);
                monthMap.put("february", 2);
                monthMap.put("march", 3);
                monthMap.put("april", 4);
                monthMap.put("may", 5);
                monthMap.put("june", 6);
                monthMap.put("july", 7);
                monthMap.put("august", 8);
                monthMap.put("september", 9);
                monthMap.put("october", 10);
                monthMap.put("november", 11);
                monthMap.put("december", 12);

                Integer month = monthMap.get(monthStr.toLowerCase());
                if (month != null && day >= 1 && day <= 31) {
                    return LocalDate.of(year, month, day);
                }
            } catch (Exception e) { /* continue */ }
        }

        return null;
    }

    private Map<LocalDate, List<JobAttachment>> filterAttachmentsByDateRange(List<JobAttachment> attachments, LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, List<JobAttachment>> result = new HashMap<>();

        for (JobAttachment attachment : attachments) {
            LocalDate attachmentDate = extractDateFromFilename(attachment.getFileName());
            if (attachmentDate != null && !attachmentDate.isBefore(startDate) && !attachmentDate.isAfter(endDate)) {
                result.computeIfAbsent(attachmentDate, k -> new ArrayList<>()).add(attachment);
            }
        }

        // Sort dates in result
        return result.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    // ==================== HELPER: Create ZIP Package ====================

    private byte[] createZipPackage(byte[] pdfBytes, Map<LocalDate, List<JobAttachment>> attachmentsByDate, String baseName) throws Exception {
        ByteArrayOutputStream zipOut = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(zipOut);

        // Add PDF report
        ZipEntry pdfEntry = new ZipEntry(baseName + ".pdf");
        zos.putNextEntry(pdfEntry);
        zos.write(pdfBytes);
        zos.closeEntry();

        // Add attachments
        int totalAttachments = 0;
        for (Map.Entry<LocalDate, List<JobAttachment>> entry : attachmentsByDate.entrySet()) {
            LocalDate date = entry.getKey();
            for (JobAttachment attachment : entry.getValue()) {
                try {
                    Resource fileResource = fileStorageService.loadFileAsResource(attachment.getFilePath());
                    if (fileResource.exists() && fileResource.isReadable()) {
                        String folderName = date.toString();
                        String fileName = attachment.getFileName();
                        ZipEntry fileEntry = new ZipEntry("attachments/" + folderName + "/" + fileName);
                        zos.putNextEntry(fileEntry);

                        try (InputStream is = fileResource.getInputStream()) {
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            while ((bytesRead = is.read(buffer)) != -1) {
                                zos.write(buffer, 0, bytesRead);
                            }
                        }
                        zos.closeEntry();
                        totalAttachments++;
                    }
                } catch (Exception e) {
                    System.err.println("Failed to add attachment: " + attachment.getFileName() + " - " + e.getMessage());
                }
            }
        }

        zos.close();
        System.out.println("ZIP created with PDF + " + totalAttachments + " attachments");
        return zipOut.toByteArray();
    }

    // ==================== ADD ATTACHMENT SUMMARY TABLE TO PDF ====================

    private void addAttachmentSummaryTable(Document document, Map<LocalDate, List<JobAttachment>> attachmentsByDate, Font boldFont, Font normalFont) throws Exception {
        if (attachmentsByDate.isEmpty()) {
            return;
        }

        document.add(Chunk.NEWLINE);
        Paragraph attTitle = new Paragraph("ATTACHMENTS SUMMARY", boldFont);
        attTitle.setAlignment(Element.ALIGN_CENTER);
        document.add(attTitle);
        document.add(Chunk.NEWLINE);

        PdfPTable attTable = new PdfPTable(3);
        attTable.setWidthPercentage(80);
        attTable.setWidths(new float[]{20, 50, 30});

        // Headers
        String[] attHeaders = {"Date", "File Name", "Size"};
        for (String header : attHeaders) {
            PdfPCell cell = new PdfPCell(new Phrase(header, boldFont));
            cell.setBackgroundColor(GrayColor.LIGHT_GRAY);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(4);
            attTable.addCell(cell);
        }

        // Data rows
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        for (Map.Entry<LocalDate, List<JobAttachment>> entry : attachmentsByDate.entrySet()) {
            LocalDate date = entry.getKey();
            for (JobAttachment att : entry.getValue()) {
                attTable.addCell(date.format(dateFormatter));
                attTable.addCell(att.getFileName());
                String sizeStr = att.getFileSize() != null ? String.format("%.2f KB", att.getFileSize() / 1024.0) : "Unknown";
                attTable.addCell(sizeStr);
            }
        }

        document.add(attTable);
        document.add(Chunk.NEWLINE);

        // Summary line
        int totalFiles = attachmentsByDate.values().stream().mapToInt(List::size).sum();
        Paragraph summary = new Paragraph("Total attachments included: " + totalFiles + " file(s)", normalFont);
        summary.setAlignment(Element.ALIGN_CENTER);
        document.add(summary);
    }

    // ==================== INDIVIDUAL REPORT PDF WITH ATTACHMENTS ====================

    private byte[] generateIndividualReportPdfWithAttachments(Employee employee, List<Timesheet> timesheets,
                                                              LocalDate startDate, LocalDate endDate,
                                                              Map<LocalDate, List<JobAttachment>> attachmentsByDate) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, out);
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Paragraph title = new Paragraph("INDIVIDUAL TIMESHEET REPORT", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(Chunk.NEWLINE);

        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);

        Paragraph empInfo = new Paragraph();
        empInfo.add(new Chunk("Employee Name: ", boldFont));
        empInfo.add(new Chunk(employee.getFirstName() + " " + employee.getLastName(), normalFont));
        empInfo.add(Chunk.NEWLINE);
        empInfo.add(new Chunk("Employee ID: ", boldFont));
        empInfo.add(new Chunk(employee.getEmployeeId() != null ? employee.getEmployeeId() : "N/A", normalFont));
        empInfo.add(Chunk.NEWLINE);
        empInfo.add(new Chunk("Position: ", boldFont));
        empInfo.add(Chunk.NEWLINE);
        empInfo.add(new Chunk("Period: ", boldFont));
        empInfo.add(new Chunk(startDate.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")) + " - " +
                endDate.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")), normalFont));
        document.add(empInfo);
        document.add(Chunk.NEWLINE);

        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(60);
        summaryTable.setSpacingBefore(10);

        double totalHours = timesheets.stream().mapToDouble(Timesheet::getTotalHours).sum();
        double totalRegularHours = timesheets.stream().mapToDouble(Timesheet::getRegularHours).sum();
        double totalOvertimeHours = timesheets.stream().mapToDouble(Timesheet::getOvertimeHours).sum();
        double totalEarnings = timesheets.stream().mapToDouble(t -> t.getEarnings() != null ? t.getEarnings() : 0).sum();
        long daysPresent = timesheets.stream().filter(t -> t.getAttendanceCode() != null &&
                (t.getAttendanceCode().equals("P") || t.getAttendanceCode().equals("WP") || t.getAttendanceCode().equals("HP"))).count();

        addSummaryRow(summaryTable, "Total Hours:", String.format("%.1f", totalHours));
        addSummaryRow(summaryTable, "Regular Hours:", String.format("%.1f", totalRegularHours));
        addSummaryRow(summaryTable, "Overtime Hours:", String.format("%.1f", totalOvertimeHours));
        addSummaryRow(summaryTable, "Total Earnings:", String.format("₵%.2f", totalEarnings));
        addSummaryRow(summaryTable, "Days Present:", String.valueOf(daysPresent));
        addSummaryRow(summaryTable, "Total Days in Period:", String.valueOf(timesheets.size()));

        document.add(summaryTable);
        document.add(Chunk.NEWLINE);

        PdfPTable dailyTable = new PdfPTable(6);
        dailyTable.setWidthPercentage(100);
        dailyTable.setWidths(new float[]{15, 15, 12, 12, 12, 20});

        String[] headers = {"Date", "Day", "Code", "NH (hrs)", "OT (hrs)", "Earnings"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, boldFont));
            cell.setBackgroundColor(GrayColor.LIGHT_GRAY);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            dailyTable.addCell(cell);
        }

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        for (Timesheet ts : timesheets) {
            dailyTable.addCell(ts.getDate().format(dateFormatter));
            dailyTable.addCell(ts.getDate().getDayOfWeek().toString().substring(0, 3));
            dailyTable.addCell(ts.getAttendanceCode() != null ? ts.getAttendanceCode() : "-");
            dailyTable.addCell(String.format("%.1f", ts.getRegularHours()));
            dailyTable.addCell(String.format("%.1f", ts.getOvertimeHours()));
            dailyTable.addCell(String.format("₵%.2f", ts.getEarnings() != null ? ts.getEarnings() : 0));
        }

        document.add(dailyTable);

        // Add attachment summary table
        addAttachmentSummaryTable(document, attachmentsByDate, boldFont, normalFont);

        document.add(Chunk.NEWLINE);
        Paragraph footer = new Paragraph("Generated on: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy HH:mm")),
                FontFactory.getFont(FontFactory.HELVETICA, 8));
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        document.close();
        return out.toByteArray();
    }

    // ==================== GENERAL REPORT PDF WITH ATTACHMENTS ====================

    private byte[] generateGeneralReportPdfWithAttachments(List<Timesheet> timesheets, LocalDate startDate,
                                                           LocalDate endDate, Long jobId, String jobName,
                                                           Map<LocalDate, List<JobAttachment>> attachmentsByDate) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, out);
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        Paragraph title = new Paragraph("GENERAL TIMESHEET REPORT", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(Chunk.NEWLINE);

        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

        Paragraph info = new Paragraph();
        info.add(new Chunk("Period: ", boldFont));
        info.add(new Chunk(startDate.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")) + " - " +
                endDate.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")), normalFont));

        if (jobId != null && jobName != null) {
            info.add(Chunk.NEWLINE);
            info.add(new Chunk("Job: ", boldFont));
            info.add(new Chunk(jobName, normalFont));
        }

        document.add(info);
        document.add(Chunk.NEWLINE);

        double totalHours = timesheets.stream().mapToDouble(Timesheet::getTotalHours).sum();
        double totalRegular = timesheets.stream().mapToDouble(Timesheet::getRegularHours).sum();
        double totalOvertime = timesheets.stream().mapToDouble(Timesheet::getOvertimeHours).sum();
        double totalEarnings = timesheets.stream().mapToDouble(t -> t.getEarnings() != null ? t.getEarnings() : 0).sum();
        long uniqueEmployees = timesheets.stream().map(t -> t.getEmployee().getId()).distinct().count();

        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(50);
        summaryTable.setSpacingBefore(10);

        addSummaryRow(summaryTable, "Total Employees:", String.valueOf(uniqueEmployees));
        addSummaryRow(summaryTable, "Total Hours:", String.format("%.1f", totalHours));
        addSummaryRow(summaryTable, "Regular Hours:", String.format("%.1f", totalRegular));
        addSummaryRow(summaryTable, "Overtime Hours:", String.format("%.1f", totalOvertime));
        addSummaryRow(summaryTable, "Total Earnings:", String.format("₵%.2f", totalEarnings));

        document.add(summaryTable);
        document.add(Chunk.NEWLINE);

        PdfPTable empTable = new PdfPTable(6);
        empTable.setWidthPercentage(100);
        empTable.setWidths(new float[]{15, 25, 15, 15, 15, 20});

        String[] empHeaders = {"Employee ID", "Employee Name", "Total Hours", "Regular", "Overtime", "Earnings"};
        for (String header : empHeaders) {
            PdfPCell cell = new PdfPCell(new Phrase(header, boldFont));
            cell.setBackgroundColor(GrayColor.LIGHT_GRAY);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            empTable.addCell(cell);
        }

        var employeeSummaries = timesheets.stream()
                .collect(Collectors.groupingBy(t -> t.getEmployee().getId()));

        for (var entry : employeeSummaries.entrySet()) {
            Employee emp = entry.getValue().get(0).getEmployee();
            double empTotal = entry.getValue().stream().mapToDouble(Timesheet::getTotalHours).sum();
            double empRegular = entry.getValue().stream().mapToDouble(Timesheet::getRegularHours).sum();
            double empOvertime = entry.getValue().stream().mapToDouble(Timesheet::getOvertimeHours).sum();
            double empEarnings = entry.getValue().stream().mapToDouble(t -> t.getEarnings() != null ? t.getEarnings() : 0).sum();

            empTable.addCell(emp.getEmployeeId() != null ? emp.getEmployeeId() : "N/A");
            empTable.addCell(emp.getFirstName() + " " + emp.getLastName());
            empTable.addCell(String.format("%.1f", empTotal));
            empTable.addCell(String.format("%.1f", empRegular));
            empTable.addCell(String.format("%.1f", empOvertime));
            empTable.addCell(String.format("₵%.2f", empEarnings));
        }

        document.add(empTable);

        // Add attachment summary table
        addAttachmentSummaryTable(document, attachmentsByDate, boldFont, normalFont);

        document.add(Chunk.NEWLINE);
        Paragraph footer = new Paragraph("Generated on: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy HH:mm")),
                FontFactory.getFont(FontFactory.HELVETICA, 8));
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        document.close();
        return out.toByteArray();
    }

    private void addSummaryRow(PdfPTable table, String label, String value) {
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11);

        PdfPCell labelCell = new PdfPCell(new Phrase(label, boldFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(4);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, normalFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(4);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    // ==================== KEEP ORIGINAL METHODS FOR COMPATIBILITY ====================

    public byte[] generateIndividualReport(Long employeeId, LocalDate startDate, LocalDate endDate) throws Exception {
        return generateIndividualReportWithAttachments(employeeId, startDate, endDate);
    }

    public byte[] generateGeneralReport(LocalDate startDate, LocalDate endDate, Long jobId) throws Exception {
        return generateGeneralReportWithAttachments(startDate, endDate, jobId);
    }
}