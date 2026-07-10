package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.Exceptions.ResourceNotFoundException;
import com.example.eac_backend_project.Service.settings.AttendanceCodeSettingsService;
import com.example.eac_backend_project.Service.settings.SettingsService;
import com.example.eac_backend_project.dto.TimesheetDTO;
import com.example.eac_backend_project.dto.TimesheetUpdateDTO;
import com.example.eac_backend_project.entity.attendance.*;
import com.example.eac_backend_project.entity.settings.AttendanceCodeSettings;
import com.example.eac_backend_project.entity.settings.SystemSettings;
import com.example.eac_backend_project.repository.*;
import com.example.eac_backend_project.repository.settings.HolidayRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class TimesheetService {

    private final TimesheetRepository timesheetRepository;
    private final EmployeeRepository employeeRepository;
    private final OverviewRepository overviewRepository;
    private final HolidayRepository holidayRepository;
    private final SettingsService settingsService;

    @Autowired
    private JobEmployeeRateRepository jobEmployeeRateRepository;

    @Autowired
    private AttendanceCodeSettingsService attendanceCodeSettingsService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobAttachmentRepository jobAttachmentRepository;

    public TimesheetService(
            HolidayRepository holidayRepository,
            OverviewRepository overviewRepository,
            EmployeeRepository employeeRepository,
            TimesheetRepository timesheetRepository,
            SettingsService settingsService
    ) {
        this.holidayRepository = holidayRepository;
        this.overviewRepository = overviewRepository;
        this.employeeRepository = employeeRepository;
        this.timesheetRepository = timesheetRepository;
        this.settingsService = settingsService;
    }

    private boolean isHoliday(LocalDate date) {
        return date != null && holidayRepository.existsByDate(date);
    }

    public List<Timesheet> getAllTimesheets() {
        return timesheetRepository.findAll();
    }

    public Timesheet getTimesheetById(Long id) {
        return timesheetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Timesheet not found with id: " + id));
    }

    public Employee getEmployeeById(Long id) {
        return employeeRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Active employee not found with id: " + id));
    }

    public boolean existsByEmployeeAndDate(Long employeeId, LocalDate date) {
        Optional<Employee> employee = employeeRepository.findByIdAndActiveTrue(employeeId);
        if (employee.isEmpty()) {
            return false;
        }
        return timesheetRepository.existsByEmployeeIdAndDate(employeeId, date);
    }

    public List<Timesheet> getTimesheetsByDateRange(LocalDate startDate, LocalDate endDate) {
        return timesheetRepository.findByDateBetween(startDate, endDate);
    }

    public List<Timesheet> getEmployeeTimesheets(Long employeeId, LocalDate startDate, LocalDate endDate) {
        return timesheetRepository.findByEmployeeIdAndDateBetween(employeeId, startDate, endDate);
    }

    public List<Timesheet> getTimesheetsByEmployeeId(Long employeeId) {
        Optional<Employee> employee = employeeRepository.findByIdAndActiveTrue(employeeId);
        if (employee.isEmpty()) {
            return Collections.emptyList();
        }
        return timesheetRepository.findByEmployeeId(employeeId);
    }

    public List<Timesheet> getTimesheetsByJobId(Long jobId) {
        return timesheetRepository.findByJobId(jobId);
    }

    public List<Timesheet> getTimesheetsByStatus(TimesheetStatus status) {
        return timesheetRepository.findByStatus(status);
    }

    public List<Timesheet> getUnverifiedTimesheets(LocalDate startDate, LocalDate endDate) {
        return timesheetRepository.findUnverifiedByDateRange(startDate, endDate);
    }

    public Long countUnverifiedTimesheets(LocalDate startDate, LocalDate endDate) {
        return timesheetRepository.countUnverifiedByDateRange(startDate, endDate);
    }

    @Transactional
    public Timesheet createTimesheet(Timesheet timesheet) {
        if (timesheet.getEmployee() == null || timesheet.getEmployee().getId() == null) {
            throw new IllegalArgumentException("Employee is required");
        }
        if (timesheet.getDate() == null) {
            throw new IllegalArgumentException("Date is required");
        }

        Long employeeId = timesheet.getEmployee().getId();
        LocalDate date = timesheet.getDate();

        Employee employee = employeeRepository.findByIdAndActiveTrue(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Active employee not found with id: " + employeeId));

        if (timesheetRepository.existsByEmployeeIdAndDate(employeeId, date)) {
            throw new IllegalArgumentException("Timesheet already exists for employee " + employeeId + " on date " + date);
        }

        Timesheet newTimesheet = new Timesheet();
        newTimesheet.setEmployee(employee);
        newTimesheet.setDate(date);

        newTimesheet.setRegularHours(timesheet.getRegularHours());
        newTimesheet.setOvertimeHours(timesheet.getOvertimeHours());
        newTimesheet.setBreakHours(timesheet.getBreakHours());

        String finalCode = normalizeAttendanceCode(date, timesheet.getAttendanceCode());
        newTimesheet.setAttendanceCode(finalCode);
        newTimesheet.setStatus(statusFromAttendanceCode(finalCode));

        applyWorkHourMultipliersByCode(newTimesheet);

        newTimesheet.setTotalHours(newTimesheet.getRegularHours() + newTimesheet.getOvertimeHours());

        newTimesheet.setEarnings(timesheet.getEarnings());
        newTimesheet.setNotes(timesheet.getNotes());

        // SOURCE DOCUMENT FIELDS
        if (timesheet.getSourceDocumentMissing() != null) {
            newTimesheet.setSourceDocumentMissing(timesheet.getSourceDocumentMissing());
        } else {
            newTimesheet.setSourceDocumentMissing(false);
        }
        newTimesheet.setSourceDocumentMissingReason(timesheet.getSourceDocumentMissingReason());
        newTimesheet.setSourceDocumentMissingFlaggedAt(timesheet.getSourceDocumentMissingFlaggedAt());
        newTimesheet.setSourceDocumentMissingFlaggedBy(timesheet.getSourceDocumentMissingFlaggedBy());
        newTimesheet.setSourceDocumentType(timesheet.getSourceDocumentType());
        newTimesheet.setSourceDocumentReference(timesheet.getSourceDocumentReference());
        newTimesheet.setVerifiedAt(timesheet.getVerifiedAt());
        newTimesheet.setVerifiedBy(timesheet.getVerifiedBy());

        if (newTimesheet.getStatus() == null) {
            newTimesheet.setStatus(TimesheetStatus.PENDING);
        }

        if (timesheet.getOverview() != null) newTimesheet.setOverview(timesheet.getOverview());
        if (timesheet.getJob() != null) newTimesheet.setJob(timesheet.getJob());

        return timesheetRepository.save(newTimesheet);
    }

    @Transactional
    public List<Timesheet> createBatchTimesheets(List<Timesheet> timesheets) {
        List<Timesheet> savedTimesheets = new ArrayList<>();
        for (Timesheet timesheet : timesheets) {
            try {
                timesheet.setId(null);
                Timesheet saved = createTimesheet(timesheet);
                savedTimesheets.add(saved);
            } catch (Exception e) {
                System.err.println("Failed to create timesheet for employee "
                        + (timesheet.getEmployee() != null ? timesheet.getEmployee().getId() : "null")
                        + " on " + timesheet.getDate() + ": " + e.getMessage());
            }
        }
        return savedTimesheets;
    }

    @Transactional
    public List<Timesheet> createBatchTimesheetsOptimized(List<Timesheet> timesheets) {
        List<Timesheet> validTimesheets = new ArrayList<>();

        for (Timesheet timesheet : timesheets) {
            try {
                if (timesheet.getEmployee() == null || timesheet.getEmployee().getId() == null) {
                    throw new IllegalArgumentException("Employee ID is required");
                }
                if (timesheet.getDate() == null) {
                    throw new IllegalArgumentException("Date is required");
                }

                Long employeeId = timesheet.getEmployee().getId();
                LocalDate date = timesheet.getDate();

                Employee employee = employeeRepository.findByIdAndActiveTrue(employeeId)
                        .orElseThrow(() -> new ResourceNotFoundException("Active employee not found: " + employeeId));

                if (timesheetRepository.existsByEmployeeIdAndDate(employeeId, date)) {
                    throw new IllegalArgumentException("Timesheet already exists for employee " + employeeId + " on date " + date);
                }

                Timesheet newTimesheet = new Timesheet();
                newTimesheet.setEmployee(employee);
                newTimesheet.setDate(date);

                // ========== FIX: Set job from employee if not already set ==========
                if (timesheet.getJob() != null) {
                    newTimesheet.setJob(timesheet.getJob());
                } else if (employee.getJob() != null) {
                    newTimesheet.setJob(employee.getJob());
                    System.out.println("✅ Auto-assigned job " + employee.getJob().getName() + " to timesheet for employee " + employee.getFirstName());
                }
                // ========== END FIX ==========

                newTimesheet.setRegularHours(timesheet.getRegularHours());
                newTimesheet.setOvertimeHours(timesheet.getOvertimeHours());
                newTimesheet.setBreakHours(timesheet.getBreakHours());

                String finalCode = normalizeAttendanceCode(date, timesheet.getAttendanceCode());
                newTimesheet.setAttendanceCode(finalCode);
                newTimesheet.setStatus(statusFromAttendanceCode(finalCode));

                newTimesheet.setEarnings(timesheet.getEarnings());
                newTimesheet.setNotes(timesheet.getNotes());

                // SOURCE DOCUMENT FIELDS
                if (timesheet.getSourceDocumentMissing() != null) {
                    newTimesheet.setSourceDocumentMissing(timesheet.getSourceDocumentMissing());
                } else {
                    newTimesheet.setSourceDocumentMissing(false);
                }
                newTimesheet.setSourceDocumentMissingReason(timesheet.getSourceDocumentMissingReason());
                newTimesheet.setSourceDocumentMissingFlaggedAt(timesheet.getSourceDocumentMissingFlaggedAt());
                newTimesheet.setSourceDocumentMissingFlaggedBy(timesheet.getSourceDocumentMissingFlaggedBy());

                applyWorkHourMultipliersByCode(newTimesheet);

                newTimesheet.setTotalHours(newTimesheet.getRegularHours() + newTimesheet.getOvertimeHours());

                validTimesheets.add(newTimesheet);

            } catch (Exception e) {
                System.err.println("Skipping invalid timesheet for employee "
                        + (timesheet.getEmployee() != null ? timesheet.getEmployee().getId() : "null")
                        + " on " + timesheet.getDate() + ": " + e.getMessage());
            }
        }

        return timesheetRepository.saveAll(validTimesheets);
    }

    @Transactional
    public Timesheet updateTimesheet(Long id, Timesheet timesheetDetails) {
        Timesheet t = getTimesheetById(id);

        if (timesheetDetails.getDate() != null) {
            t.setDate(timesheetDetails.getDate());
        }
        LocalDate date = t.getDate();

        String rawCode = timesheetDetails.getAttendanceCode();
        String finalCode = normalizeAttendanceCode(date, rawCode);
        t.setAttendanceCode(finalCode);
        t.setStatus(statusFromAttendanceCode(finalCode));

        t.setRegularHours(timesheetDetails.getRegularHours());
        t.setOvertimeHours(timesheetDetails.getOvertimeHours());
        t.setBreakHours(timesheetDetails.getBreakHours());

        applyWorkHourMultipliersByCode(t);

        t.setTotalHours(t.getRegularHours() + t.getOvertimeHours());

        t.setEarnings(timesheetDetails.getEarnings());
        t.setNotes(timesheetDetails.getNotes());

        // SOURCE DOCUMENT FIELDS - update if provided
        if (timesheetDetails.getSourceDocumentMissing() != null) {
            t.setSourceDocumentMissing(timesheetDetails.getSourceDocumentMissing());
        }
        if (timesheetDetails.getSourceDocumentMissingReason() != null) {
            t.setSourceDocumentMissingReason(timesheetDetails.getSourceDocumentMissingReason());
        }
        if (timesheetDetails.getSourceDocumentMissingFlaggedAt() != null) {
            t.setSourceDocumentMissingFlaggedAt(timesheetDetails.getSourceDocumentMissingFlaggedAt());
        }
        if (timesheetDetails.getSourceDocumentMissingFlaggedBy() != null) {
            t.setSourceDocumentMissingFlaggedBy(timesheetDetails.getSourceDocumentMissingFlaggedBy());
        }
        if (timesheetDetails.getSourceDocumentType() != null) {
            t.setSourceDocumentType(timesheetDetails.getSourceDocumentType());
        }
        if (timesheetDetails.getSourceDocumentReference() != null) {
            t.setSourceDocumentReference(timesheetDetails.getSourceDocumentReference());
        }

        if (timesheetDetails.getJob() != null) t.setJob(timesheetDetails.getJob());
        if (timesheetDetails.getOverview() != null) t.setOverview(timesheetDetails.getOverview());

        return timesheetRepository.save(t);
    }

    @Transactional
    public Timesheet updateTimesheetPartial(Long id, TimesheetUpdateDTO dto) {
        Timesheet t = getTimesheetById(id);

        if (dto.getDate() != null) {
            t.setDate(dto.getDate());
        }

        if (dto.getAttendanceCode() != null) {
            String finalCode = normalizeAttendanceCode(t.getDate(), dto.getAttendanceCode());
            t.setAttendanceCode(finalCode);
            t.setStatus(statusFromAttendanceCode(finalCode));
        }

        if (dto.getRegularHours() != null) {
            t.setRegularHours(dto.getRegularHours());
        }

        if (dto.getOvertimeHours() != null) {
            t.setOvertimeHours(dto.getOvertimeHours());
        }

        if (dto.getBreakHours() != null) {
            t.setBreakHours(dto.getBreakHours());
        }

        if (dto.getAttendanceCode() != null || dto.getRegularHours() != null || dto.getOvertimeHours() != null) {
            applyWorkHourMultipliersByCode(t);
            t.setTotalHours(t.getRegularHours() + t.getOvertimeHours());
        }

        if (dto.getEarnings() != null) {
            t.setEarnings(dto.getEarnings());
        }

        if (dto.getNotes() != null) {
            t.setNotes(dto.getNotes());
        }

        // SOURCE DOCUMENT FIELDS - partial update
        if (dto.getSourceDocumentMissing() != null) {
            t.setSourceDocumentMissing(dto.getSourceDocumentMissing());
        }
        if (dto.getSourceDocumentMissingReason() != null) {
            t.setSourceDocumentMissingReason(dto.getSourceDocumentMissingReason());
        }
        if (dto.getSourceDocumentMissingFlaggedBy() != null) {
            t.setSourceDocumentMissingFlaggedBy(dto.getSourceDocumentMissingFlaggedBy());
            t.setSourceDocumentMissingFlaggedAt(LocalDateTime.now());
        }

        return timesheetRepository.save(t);
    }

    @Transactional
    public void deleteTimesheet(Long id) {
        Timesheet timesheet = getTimesheetById(id);
        timesheetRepository.delete(timesheet);
    }

    @Transactional
    public Timesheet updateTimesheetStatus(Long id, String status) {
        Timesheet timesheet = getTimesheetById(id);
        try {
            TimesheetStatus newStatus = TimesheetStatus.valueOf(status.toUpperCase());
            timesheet.setStatus(newStatus);
            return timesheetRepository.save(timesheet);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }
    }

    @Transactional
    public List<Timesheet> bulkUpdateStatus(List<Long> ids, String status) {
        List<Timesheet> updatedTimesheets = new ArrayList<>();
        for (Long id : ids) {
            try {
                Timesheet updated = updateTimesheetStatus(id, status);
                updatedTimesheets.add(updated);
            } catch (Exception e) {
                System.err.println("Failed to update timesheet " + id + ": " + e.getMessage());
            }
        }
        return updatedTimesheets;
    }

    @Transactional
    public int bulkMarkSourceDocumentMissing(List<Long> employeeIds, LocalDate startDate, LocalDate endDate,
                                             Boolean missing, String reason, String flaggedBy) {
        return timesheetRepository.bulkUpdateSourceDocumentStatus(
                employeeIds, startDate, endDate, missing, reason, LocalDateTime.now(), flaggedBy
        );
    }

    @Transactional
    public int markSingleEntrySourceDocumentMissing(Long timesheetId, Boolean missing, String reason) {
        return timesheetRepository.updateSourceDocumentStatusById(timesheetId, missing, reason);
    }

    @Transactional
    public Timesheet verifyTimesheet(Long id, String verifiedBy) {
        Timesheet t = getTimesheetById(id);
        t.setVerifiedAt(LocalDateTime.now());
        t.setVerifiedBy(verifiedBy);
        t.setSourceDocumentMissing(false);
        return timesheetRepository.save(t);
    }

    @Transactional
    public List<Timesheet> createTimesheetsFromPayload(List<Map<String, Object>> timesheetsData) {
        List<Timesheet> timesheets = new ArrayList<>();

        for (Map<String, Object> data : timesheetsData) {
            try {
                Long employeeId = ((Number) data.get("employeeId")).longValue();
                LocalDate date = LocalDate.parse((String) data.get("date"));

                if (timesheetRepository.existsByEmployeeIdAndDate(employeeId, date)) {
                    System.err.println("Timesheet already exists for employee " + employeeId + " on " + date + ", skipping...");
                    continue;
                }

                Employee employee = getEmployeeById(employeeId);

                Timesheet timesheet = new Timesheet();
                timesheet.setEmployee(employee);
                timesheet.setDate(date);

                timesheet.setRegularHours(num(data, "regularHours", 0.0));
                timesheet.setOvertimeHours(num(data, "overtimeHours", 0.0));
                timesheet.setBreakHours(num(data, "breakHours", 0.0));

                timesheet.setTotalHours(timesheet.getRegularHours() + timesheet.getOvertimeHours());

                if (data.containsKey("earnings")) {
                    Object ev = data.get("earnings");
                    timesheet.setEarnings(ev == null ? null : num(data, "earnings", 0.0));
                }

                String raw = data.get("attendanceCode") == null ? null : data.get("attendanceCode").toString();
                String finalCode = normalizeAttendanceCode(date, raw);
                timesheet.setAttendanceCode(finalCode);
                timesheet.setStatus(statusFromAttendanceCode(finalCode));

                if (data.containsKey("notes")) {
                    timesheet.setNotes(data.get("notes") == null ? null : data.get("notes").toString());
                }

                // SOURCE DOCUMENT FIELDS from payload
                if (data.containsKey("sourceDocumentMissing")) {
                    timesheet.setSourceDocumentMissing(Boolean.valueOf(data.get("sourceDocumentMissing").toString()));
                }
                if (data.containsKey("sourceDocumentMissingReason")) {
                    timesheet.setSourceDocumentMissingReason(data.get("sourceDocumentMissingReason").toString());
                }
                if (data.containsKey("sourceDocumentType")) {
                    timesheet.setSourceDocumentType(data.get("sourceDocumentType").toString());
                }
                if (data.containsKey("sourceDocumentReference")) {
                    timesheet.setSourceDocumentReference(data.get("sourceDocumentReference").toString());
                }

                applyWorkHourMultipliersByCode(timesheet);
                timesheet.setTotalHours(timesheet.getRegularHours() + timesheet.getOvertimeHours());

                timesheets.add(timesheet);

            } catch (Exception e) {
                System.err.println("Failed to process timesheet data: " + data + ", error: " + e.getMessage());
                e.printStackTrace();
            }
        }

        return createBatchTimesheetsOptimized(timesheets);
    }

    @Transactional
    public List<Timesheet> createTimesheetsFromDTO(List<TimesheetDTO> timesheetDTOs) {
        List<Timesheet> timesheets = new ArrayList<>();
        for (TimesheetDTO dto : timesheetDTOs) {
            try {
                if (timesheetRepository.existsByEmployeeIdAndDate(dto.getEmployeeId(), dto.getDate())) {
                    continue;
                }

                Employee employee = getEmployeeById(dto.getEmployeeId());

                Timesheet timesheet = new Timesheet();
                timesheet.setEmployee(employee);
                timesheet.setDate(dto.getDate());
                timesheet.setRegularHours(dto.getRegularHours());
                timesheet.setOvertimeHours(dto.getOvertimeHours());
                timesheet.setBreakHours(dto.getBreakHours() != null ? dto.getBreakHours() : 0.0);

                double total = (dto.getTotalHours() != null)
                        ? dto.getTotalHours()
                        : (dto.getRegularHours() + dto.getOvertimeHours());
                timesheet.setTotalHours(total);

                timesheet.setEarnings(dto.getEarnings());
                timesheet.setNotes(dto.getNotes());

                // SOURCE DOCUMENT FIELDS from DTO
                timesheet.setSourceDocumentMissing(dto.getSourceDocumentMissing());
                timesheet.setSourceDocumentMissingReason(dto.getSourceDocumentMissingReason());
                timesheet.setSourceDocumentType(dto.getSourceDocumentType());
                timesheet.setSourceDocumentReference(dto.getSourceDocumentReference());

                if (dto.getStatus() != null) {
                    timesheet.setStatus(TimesheetStatus.valueOf(dto.getStatus().toUpperCase()));
                } else {
                    timesheet.setStatus(TimesheetStatus.PENDING);
                }

                timesheets.add(timesheet);

            } catch (Exception e) {
                System.err.println("Failed to process timesheet DTO: " + dto + ", error: " + e.getMessage());
            }
        }

        return timesheetRepository.saveAll(timesheets);
    }

    @Transactional
    public Map<String, Object> upsertTimesheetsFromPayload(List<Map<String, Object>> timesheetsData, String saveMode) {
        String mode = (saveMode == null || saveMode.isBlank()) ? "skip" : saveMode.trim().toLowerCase();

        int created = 0;
        int updated = 0;
        int skipped = 0;
        int failed = 0;

        List<Timesheet> saved = new ArrayList<>();

        for (Map<String, Object> data : timesheetsData) {
            try {
                // Check for null and handle safely
                if (data.get("employeeId") == null) throw new IllegalArgumentException("employeeId is required");
                if (data.get("date") == null) throw new IllegalArgumentException("date is required");

                Long employeeId = ((Number) data.get("employeeId")).longValue();

                // Safe date parsing
                Object dateObj = data.get("date");
                LocalDate date;

                if (dateObj instanceof LocalDate) {
                    date = (LocalDate) dateObj;
                } else if (dateObj instanceof String) {
                    date = LocalDate.parse((String) dateObj);
                } else if (dateObj instanceof java.sql.Date) {
                    date = ((java.sql.Date) dateObj).toLocalDate();
                } else if (dateObj instanceof java.util.Date) {
                    date = ((java.util.Date) dateObj).toInstant()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate();
                } else {
                    date = LocalDate.parse(dateObj.toString());
                }

                Employee employee = getEmployeeById(employeeId);

                // ========== SAFE: Process jobId ONLY if present (OPTIONAL) ==========
                Long jobId = null;
                if (data.containsKey("jobId") && data.get("jobId") != null) {
                    try {
                        jobId = ((Number) data.get("jobId")).longValue();
                    } catch (Exception e) {
                        System.out.println("⚠️ Invalid jobId format, ignoring: " + data.get("jobId"));
                    }
                }

                // Fetch job ONLY if jobId is provided and valid
                Job job = null;
                if (jobId != null && jobId > 0) {
                    job = jobRepository.findById(jobId).orElse(null);
                    if (job == null) {
                        System.out.println("⚠️ Job not found for ID: " + jobId + " - will use employee's job if available");
                    }
                }
                // ========== END OF SAFE ADDITION ==========

                Timesheet target = timesheetRepository.findByEmployeeIdAndDate(employeeId, date).orElse(null);

                if (target != null && !"override".equals(mode)) {
                    skipped++;
                    continue;
                }

                if (target == null) {
                    target = new Timesheet();
                    target.setEmployee(employee);
                    target.setDate(date);
                    created++;
                } else {
                    updated++;
                }

                // ========== SAFE: Set job ONLY if we have one (preserve existing if not) ==========
                if (job != null) {
                    // Explicitly provided job from payload takes priority
                    target.setJob(job);
                    System.out.println("✅ Setting job from payload: " + job.getName() + " (ID: " + job.getId() + ")");
                } else if (target.getJob() == null && employee.getJob() != null) {
                    // Fallback: use employee's job only if no job is set and employee has one
                    target.setJob(employee.getJob());
                    System.out.println("✅ Using fallback - employee's job: " + employee.getJob().getName());
                } else {
                    // Keep existing job or leave as null - perfectly acceptable
                    System.out.println("ℹ️ No job set for timesheet (this is allowed)");
                }
                // ========== END OF SAFE ADDITION ==========

                // Safe attendance code handling
                if (data.containsKey("attendanceCode") && data.get("attendanceCode") != null) {
                    String raw = data.get("attendanceCode").toString();
                    String finalCode = normalizeAttendanceCode(date, raw);
                    target.setAttendanceCode(finalCode);
                    target.setStatus(statusFromAttendanceCode(finalCode));
                } else if (target.getStatus() == null) {
                    target.setStatus(TimesheetStatus.PENDING);
                }

                // Safe numeric parsing for all numeric fields
                double regularHours = num(data, "regularHours", 0.0);
                double overtimeHours = num(data, "overtimeHours", 0.0);
                double breakHours = num(data, "breakHours", 0.0);

                target.setRegularHours(regularHours);
                target.setOvertimeHours(overtimeHours);
                target.setBreakHours(breakHours);

                applyWorkHourMultipliersByCode(target);
                target.setTotalHours(target.getRegularHours() + target.getOvertimeHours());

                // Safe earnings handling
                if (data.containsKey("earnings") && data.get("earnings") != null) {
                    target.setEarnings(num(data, "earnings", 0.0));
                }

                // Safe notes handling
                if (data.containsKey("notes") && data.get("notes") != null) {
                    target.setNotes(data.get("notes").toString());
                }

                // SOURCE DOCUMENT FIELDS - with null safety
                if (data.containsKey("sourceDocumentMissing") && data.get("sourceDocumentMissing") != null) {
                    target.setSourceDocumentMissing(Boolean.valueOf(data.get("sourceDocumentMissing").toString()));
                }

                if (data.containsKey("sourceDocumentMissingReason") && data.get("sourceDocumentMissingReason") != null) {
                    target.setSourceDocumentMissingReason(data.get("sourceDocumentMissingReason").toString());
                }

                if (data.containsKey("sourceDocumentMissingFlaggedBy") && data.get("sourceDocumentMissingFlaggedBy") != null) {
                    target.setSourceDocumentMissingFlaggedBy(data.get("sourceDocumentMissingFlaggedBy").toString());
                    target.setSourceDocumentMissingFlaggedAt(LocalDateTime.now());
                }

                if (data.containsKey("sourceDocumentType") && data.get("sourceDocumentType") != null) {
                    target.setSourceDocumentType(data.get("sourceDocumentType").toString());
                }

                if (data.containsKey("sourceDocumentReference") && data.get("sourceDocumentReference") != null) {
                    target.setSourceDocumentReference(data.get("sourceDocumentReference").toString());
                }

                Timesheet savedRow = timesheetRepository.save(target);
                saved.add(savedRow);

                // Log for debugging
                System.out.println("💾 Saved timesheet - ID: " + savedRow.getId() +
                        ", Employee: " + employee.getFirstName() +
                        " " + employee.getLastName() +
                        ", Date: " + date +
                        ", Job: " + (savedRow.getJob() != null ? savedRow.getJob().getName() : "NULL (allowed)"));

            } catch (Exception e) {
                failed++;
                System.err.println("Upsert failed for payload row: " + data + " => " + e.getMessage());
                e.printStackTrace();
            }
        }

        return Map.of(
                "success", failed == 0,
                "mode", mode,
                "createdCount", created,
                "updatedCount", updated,
                "skippedCount", skipped,
                "failedCount", failed,
                "savedCount", saved.size(),
                "savedTimesheets", saved
        );
    }

    private String normalizeAttendanceCode(LocalDate date, String raw) {
        String code = (raw == null) ? null : raw.trim();

        if (code != null && code.isEmpty()) code = null;

        if (code != null && (code.equalsIgnoreCase("O") || code.equals("0"))) {
            code = "O";
        }

        if (date != null && isHoliday(date)) {
            if (code == null || code.isBlank() || code.equalsIgnoreCase("O")) return "H";
            if (code.equalsIgnoreCase("P") || code.equalsIgnoreCase("HP")) return "HP";
            return "H";
        }

        return (code == null) ? null : code.toUpperCase();
    }

    private TimesheetStatus statusFromAttendanceCode(String code) {
        if (code == null) return TimesheetStatus.PENDING;
        String c = code.trim().toUpperCase();
        if (c.isEmpty()) return TimesheetStatus.PENDING;

        if (c.equals("P") || c.equals("WP") || c.equals("HP")) return TimesheetStatus.PENDING;
        if (c.equals("A") || c.equals("OFF")) return TimesheetStatus.REJECTED;
        if (c.equals("L") || c.equals("S")) return TimesheetStatus.APPROVED;

        return TimesheetStatus.PENDING;
    }

    private double num(Map<String, Object> data, String key, double def) {
        Object v = data.get(key);
        if (v == null) return def;
        if (v instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(v.toString());
        } catch (Exception e) {
            return def;
        }
    }

    private void applyWorkHourMultipliersByCode(Timesheet t) {
        if (t == null) return;

        SystemSettings s = settingsService.getSystemSettings();
        AttendanceCodeSettings codeSettings = attendanceCodeSettingsService.getSettings(t.getAttendanceCode());

        double standardHours = s.getStandardWorkHours() != null ? s.getStandardWorkHours() : 8.0;
        double defaultOtMult = s.getDefaultOvertimeMultiplier() != null ? s.getDefaultOvertimeMultiplier() : 1.0;
        double holidayMult = s.getHolidayOvertimeMultiplier() != null ? s.getHolidayOvertimeMultiplier() : defaultOtMult;

        String code = t.getAttendanceCode() == null ? "" : t.getAttendanceCode().trim().toUpperCase();

        double rawOt = t.getOvertimeHours();
        double rawRh = t.getRegularHours();

        // Use settings from database
        boolean allowOT = codeSettings != null && codeSettings.getAllowOT() != null && codeSettings.getAllowOT();
        boolean capNH = codeSettings != null && codeSettings.getCapNH() != null && codeSettings.getCapNH();
        double defaultNH = codeSettings != null && codeSettings.getDefaultNH() != null ? codeSettings.getDefaultNH() : standardHours;

        if ("P".equals(code) || "WP".equals(code)) {
            double effectiveOt = allowOT ? rawOt * defaultOtMult : 0;
            t.setOvertimeHours(effectiveOt);
            t.setTotalHours(standardHours + effectiveOt);
            return;
        }

        if ("HP".equals(code)) {
            double nh = capNH ? Math.min(defaultNH, standardHours) : defaultNH;
            double holidayCredited = nh * holidayMult;
            double totalCredited = holidayCredited * defaultOtMult;
            double ot = allowOT ? Math.max(0, totalCredited - nh) : 0;

            t.setRegularHours(nh);
            t.setOvertimeHours(ot);
            t.setTotalHours(nh + ot);
            return;
        }

        t.setTotalHours(rawRh + rawOt);
    }

    @Transactional
    public int deleteByEmployeeIdsAndDate(List<Long> employeeIds, LocalDate date) {
        return timesheetRepository.deleteByEmployeeIdInAndDate(employeeIds, date);
    }

    @Transactional
    public int deleteByEmployeeIdsAndDateRange(List<Long> employeeIds, LocalDate startDate, LocalDate endDate) {
        return timesheetRepository.deleteByEmployeeIdInAndDateBetween(employeeIds, startDate, endDate);
    }

    @Transactional
    public int deleteByEmployeeIds(List<Long> employeeIds) {
        return timesheetRepository.deleteByEmployeeIdIn(employeeIds);
    }

    @Transactional
    public int deleteByDate(LocalDate date) {
        return timesheetRepository.deleteByDate(date);
    }

    @Transactional
    public int deleteByDateRange(LocalDate startDate, LocalDate endDate) {
        return timesheetRepository.deleteByDateBetween(startDate, endDate);
    }

    @Transactional
    public int deleteAll() {
        int count = (int) timesheetRepository.count();
        timesheetRepository.deleteAll();
        return count;
    }

    // Add this method to your TimesheetService class

    /**
     * Get timesheets for a specific job within date range that have hours > 0
     */
    public List<Timesheet> getTimesheetsByJobAndDateRangeWithHours(Long jobId, LocalDate startDate, LocalDate endDate) {
        return timesheetRepository.findByJobIdAndDateBetweenWithHours(jobId, startDate, endDate);
    }

    /**
     * Generate a ZIP file containing:
     * - Excel invoice (from generateJobInvoiceExcel)
     * - All job attachments whose documentDate falls within the given range
     */
    public byte[] generateJobInvoiceZip(Long jobId, LocalDate startDate, LocalDate endDate) throws Exception {
        // 1. Generate the Excel invoice
        byte[] excelData = generateJobInvoiceExcel(jobId, startDate, endDate);

        // 2. Fetch attachments for this job in the date range
        List<JobAttachment> attachments = jobAttachmentRepository.findByJobIdAndDocumentDateBetween(jobId, startDate, endDate);

        // 3. Create the ZIP in memory
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // Add the Excel file
            ZipEntry excelEntry = new ZipEntry("Invoice_" + jobId + "_" + startDate + "_to_" + endDate + ".xlsx");
            zos.putNextEntry(excelEntry);
            zos.write(excelData);
            zos.closeEntry();

            // Add each attachment
            for (JobAttachment att : attachments) {
                File file = new File(att.getFilePath());
                if (file.exists() && file.isFile()) {
                    // Use a subfolder to keep the ZIP clean
                    String entryName = "attachments/" + att.getFileName();
                    ZipEntry fileEntry = new ZipEntry(entryName);
                    zos.putNextEntry(fileEntry);

                    try (FileInputStream fis = new FileInputStream(file)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            zos.write(buffer, 0, bytesRead);
                        }
                    }
                    zos.closeEntry();
                } else {
                    // Log warning but don't fail the whole ZIP
                    System.err.println("Attachment file missing: " + att.getFilePath());
                }
            }
        }

        return baos.toByteArray();
    }

    /**
     * Generate Excel invoice for a job within date range.
     * Format:
     *   Employee Name | Practice Title | Rate (₵/hr) | Date1 | Date2 | ... | Total Hours | Amount (₵)
     * Two rows per employee: NH row (Practice Title = "Normal Work"), OT row (Practice Title = "Overtime").
     * Uses:
     *   - Job‑specific normal rate (JobEmployeeRate.normalHourRate) if available, else employee default.
     *   - Job‑specific overtime rate (JobEmployeeRate.overtimeRate) if available, else:
     *        job’s overtimeMultiplier * normal rate (if job multiplier is set),
     *        else system default multiplier * normal rate.
     */
    public byte[] generateJobInvoiceExcel(Long jobId, LocalDate startDate, LocalDate endDate) throws Exception {
        // Fetch job and its overtime multiplier
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));

        // Fetch timesheets for this job within date range that have hours
        List<Timesheet> timesheets = getTimesheetsByJobAndDateRangeWithHours(jobId, startDate, endDate);
        if (timesheets.isEmpty()) {
            throw new RuntimeException("No timesheet data found for job " + job.getName() + " in the selected period");
        }

        // Group timesheets by employee
        Map<Long, List<Timesheet>> employeeTimesheets = new HashMap<>();
        for (Timesheet ts : timesheets) {
            employeeTimesheets.computeIfAbsent(ts.getEmployee().getId(), k -> new ArrayList<>()).add(ts);
        }

        // Get all dates in the range
        List<LocalDate> datesInRange = new ArrayList<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            datesInRange.add(current);
            current = current.plusDays(1);
        }

        // Create workbook and sheet
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Invoice");

        // Styles
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle boldDataStyle = createBoldDataStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);

        int rowNum = 0;

        // ========== HEADER ROW ==========
        Row headerRow = sheet.createRow(rowNum++);
        int col = 0;

        String[] headers = {"Employee Name", "Practice Title", "Rate (₵/hr)"};
        for (String h : headers) {
            Cell cell = headerRow.createCell(col++);
            cell.setCellValue(h);
            cell.setCellStyle(headerStyle);
        }

        // Date columns
        for (LocalDate date : datesInRange) {
            Cell dateCell = headerRow.createCell(col++);
            dateCell.setCellValue(date.format(DateTimeFormatter.ofPattern("dd-MMM")));
            dateCell.setCellStyle(headerStyle);
        }

        // Total Hours
        Cell totalHeader = headerRow.createCell(col++);
        totalHeader.setCellValue("Total Hours");
        totalHeader.setCellStyle(headerStyle);

        // Amount
        Cell amountHeader = headerRow.createCell(col);
        amountHeader.setCellValue("Amount (₵)");
        amountHeader.setCellStyle(headerStyle);

        // Pre‑build timesheet map: key = employeeId_date
        Map<String, Timesheet> timesheetMap = new HashMap<>();
        for (Timesheet ts : timesheets) {
            String key = ts.getEmployee().getId() + "_" + ts.getDate().toString();
            timesheetMap.put(key, ts);
        }

        // ========== DATA ROWS ==========
        for (Map.Entry<Long, List<Timesheet>> entry : employeeTimesheets.entrySet()) {
            Long employeeId = entry.getKey();
            Employee employee = employeeRepository.findById(employeeId).orElse(null);
            if (employee == null) continue;

            // ----- Get job‑specific rates from JobEmployeeRate -----
            JobEmployeeRate jobRate = jobEmployeeRateRepository
                    .findByJobIdAndEmployeeId(jobId, employeeId)
                    .orElse(null);

            double normalRate;
            if (jobRate != null && jobRate.getNormalHourRate() != null) {
                normalRate = jobRate.getNormalHourRate();
            } else {
                normalRate = employee.getMinimumRate() != null ? employee.getMinimumRate()
                        : (employee.getHourlyRate() != null ? employee.getHourlyRate() : 0.0);
            }

            double overtimeRate;
            if (jobRate != null && jobRate.getOvertimeRate() != null) {
                // Employee‑specific overtime rate takes highest priority
                overtimeRate = jobRate.getOvertimeRate();
            } else {
                // Use job's overtime multiplier if set, otherwise system default
                Double jobMult = job.getOvertimeMultiplier();
                Double sysMult = settingsService.getSystemSettings().getDefaultOvertimeMultiplier();
                double mult = (jobMult != null) ? jobMult : (sysMult != null ? sysMult : 1.5);
                overtimeRate = normalRate * mult;
            }

            // ====== NH ROW (Normal Work) ======
            Row nhRow = sheet.createRow(rowNum++);
            col = 0;

            // Employee Name
            nhRow.createCell(col++).setCellValue(employee.getFirstName() + " " + employee.getLastName());
            // Practice Title
            nhRow.createCell(col++).setCellValue("Normal Work");
            // Rate
            nhRow.createCell(col++).setCellValue(normalRate);

            double totalNh = 0;
            for (LocalDate date : datesInRange) {
                String key = employeeId + "_" + date.toString();
                Timesheet ts = timesheetMap.get(key);
                double nh = (ts != null) ? ts.getRegularHours() : 0;
                nhRow.createCell(col++).setCellValue(nh);
                totalNh += nh;
            }

            // Total Hours
            Cell totalNhCell = nhRow.createCell(col++);
            totalNhCell.setCellValue(totalNh);
            totalNhCell.setCellStyle(boldDataStyle);

            // Amount = Total NH × Normal Rate
            double nhAmount = totalNh * normalRate;
            Cell amountNhCell = nhRow.createCell(col++);
            amountNhCell.setCellValue(nhAmount);
            amountNhCell.setCellStyle(currencyStyle);

            // ====== OT ROW (Overtime) ======
            Row otRow = sheet.createRow(rowNum++);
            col = 0;

            // Employee Name – blank
            otRow.createCell(col++).setCellValue("");
            // Practice Title
            otRow.createCell(col++).setCellValue("Overtime");
            // Rate – Overtime Rate
            otRow.createCell(col++).setCellValue(overtimeRate);

            double totalOt = 0;
            for (LocalDate date : datesInRange) {
                String key = employeeId + "_" + date.toString();
                Timesheet ts = timesheetMap.get(key);
                double ot = (ts != null) ? ts.getOvertimeHours() : 0;
                otRow.createCell(col++).setCellValue(ot);
                totalOt += ot;
            }

            // Total Hours
            Cell totalOtCell = otRow.createCell(col++);
            totalOtCell.setCellValue(totalOt);
            totalOtCell.setCellStyle(boldDataStyle);

            // Amount = Total OT × Overtime Rate
            double otAmount = totalOt * overtimeRate;
            Cell amountOtCell = otRow.createCell(col++);
            amountOtCell.setCellValue(otAmount);
            amountOtCell.setCellStyle(currencyStyle);
        }

        // ========== LEGEND ROW ==========
        rowNum++;
        Row legendRow = sheet.createRow(rowNum);
        Cell legendCell = legendRow.createCell(0);
        legendCell.setCellValue("Legend: NH = Normal Work, OT = Overtime. Amount = Total Hours × Rate.");
        legendCell.setCellStyle(dataStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, 2 + datesInRange.size() + 1)); // +1 for Amount

        // Auto‑size columns
        for (int i = 0; i <= 2 + datesInRange.size() + 1; i++) {
            sheet.autoSizeColumn(i);
            if (sheet.getColumnWidth(i) < 3000) {
                sheet.setColumnWidth(i, 3000);
            }
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }

    // Helper method for currency style
    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        DataFormat df = workbook.createDataFormat();
        style.setDataFormat(df.getFormat("₵#,##0.00")); // Ghana cedis format
        return style;
    }

    public void debugTimesheetJobLinks() {
        List<Timesheet> allTimesheets = timesheetRepository.findAll();
        System.out.println("=== Checking Timesheet Job Links ===");
        System.out.println("Total timesheets: " + allTimesheets.size());

        int withJob = 0;
        int withoutJob = 0;

        for (Timesheet ts : allTimesheets) {
            if (ts.getJob() != null && ts.getJob().getId() != null) {
                withJob++;
                System.out.println("✓ Timesheet ID " + ts.getId() + " - Job: " + ts.getJob().getName() + " (ID: " + ts.getJob().getId() + ")");
            } else {
                withoutJob++;
                System.out.println("✗ Timesheet ID " + ts.getId() + " - NO JOB LINKED");
            }
        }

        System.out.println("Timesheets WITH job: " + withJob);
        System.out.println("Timesheets WITHOUT job: " + withoutJob);
    }

    // Helper methods for cell styles
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createSubHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createBoldDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createTotalStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    @Transactional
    public int deleteTimesheetsByJobId(Long jobId) {
        return timesheetRepository.deleteByJobId(jobId);
    }
}