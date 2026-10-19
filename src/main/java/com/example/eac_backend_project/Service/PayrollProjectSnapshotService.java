package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.entity.attendance.*;
import com.example.eac_backend_project.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Predicate;

@Service
public class PayrollProjectSnapshotService {
    private final AttendanceRepository attendance;
    private final TimesheetRepository timesheets;
    private final ObjectMapper mapper;
    public PayrollProjectSnapshotService(AttendanceRepository attendance, TimesheetRepository timesheets, ObjectMapper mapper) {
        this.attendance = attendance;
        this.timesheets = timesheets;
        this.mapper = mapper;
    }
    public void capture(PayrollRecord record, Map<LocalDate, Double> daily, Map<LocalDate, Double> overtime,
                        double normalHours, double standardHours, boolean convertExcess,
                        Predicate<LocalDate> automaticWeekend, double weekendMultiplier) {
        PayrollPeriod period = record.getPeriod();
        Map<LocalDate, PayrollProjectEarnings.Work> work = new HashMap<>();
        Map<LocalDate, Timesheet> datedTimesheets = new HashMap<>();
        for (Timesheet sheet : timesheets.findByEmployeeIdAndDateBetween(record.getEmployee().getId(), period.getStartDate(), period.getEndDate())) {
            datedTimesheets.put(sheet.getDate(), sheet);
        }
        for (Attendance entry : attendance.findByEmployeeIdAndDateBetween(record.getEmployee().getId(), period.getStartDate(), period.getEndDate())) {
            boolean synced = "TIMESHEET_SYNC".equals(entry.getMethod());
            Long projectId = entry.getProjectId();
            String projectName = entry.getProjectName();
            Long timesheetId = entry.getTimesheetId();
            // Legacy synced attendance can be matched at generation time only.
            // Never infer historical work from the employee's current project.
            if (synced && projectId == null && timesheetId == null) {
                Timesheet sheet = datedTimesheets.get(entry.getDate());
                if (sheet != null) {
                    timesheetId = sheet.getId();
                    if (sheet.getJob() != null) {
                        projectId = sheet.getJob().getId();
                        projectName = sheet.getJob().getName();
                    }
                }
            }
            work.put(entry.getDate(), new PayrollProjectEarnings.Work(timesheetId, projectId, projectName,
                    synced ? "Timesheet" : entry.isBiometric() ? "Biometric" : "Attendance"));
        }
        try {
            record.setProjectEarningsSnapshot(mapper.writeValueAsString(PayrollProjectEarnings.build(record,
                    daily, overtime, work, normalHours, standardHours, convertExcess, automaticWeekend, weekendMultiplier)));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not save payroll project earnings", e);
        }
    }
}
