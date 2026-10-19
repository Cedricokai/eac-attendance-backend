package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.entity.attendance.*;
import com.example.eac_backend_project.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.*;
import java.util.*;

@Service
public class TimesheetPayrollApprovalService {
    private final TimesheetRepository timesheets;
    private final AttendanceRepository attendance;
    private final PayrollPeriodRepository periods;
    @org.springframework.beans.factory.annotation.Autowired
    private PayrollWorkScopeService payrollWorkScopeService;
    public TimesheetPayrollApprovalService(TimesheetRepository timesheets, AttendanceRepository attendance, PayrollPeriodRepository periods) {
        this.timesheets = timesheets; this.attendance = attendance; this.periods = periods;
    }
    private Authentication actor() {
        Authentication actor = SecurityContextHolder.getContext().getAuthentication();
        if (actor == null || !actor.isAuthenticated() || "anonymousUser".equals(actor.getName()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to review timesheets");
        return actor;
    }
    public boolean canApprove() {
        return actor().getAuthorities().stream().anyMatch(a -> Set.of("ROLE_ADMIN", "ROLE_HR", "ROLE_SUPERVISOR", "ROLE_PLANNER").contains(a.getAuthority()));
    }
    @Transactional
    public List<Map<String,Object>> decide(List<Long> ids, String action, String reason) {
        Authentication actor = actor();
        if (!Set.of("SUBMIT", "APPROVE", "REJECT").contains(action)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid approval action");
        if (!"SUBMIT".equals(action) && !canApprove()) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only HR, supervisors, planners and administrators can approve project timesheets");
        if (ids == null || ids.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Select saved timesheets");
        List<Timesheet> rows = new ArrayList<>();
        for (Long id : new LinkedHashSet<>(ids)) {
            Timesheet t = timesheets.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Timesheet not found"));
            if (t.getJob() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Only project timesheets use this approval workflow");
            if (!Double.isFinite(t.getRegularHours()) || !Double.isFinite(t.getOvertimeHours()) || t.getRegularHours() < 0 || t.getOvertimeHours() < 0)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Timesheet hours must be finite and nonnegative");
            t.invalidateChangedPayrollApproval();
            if ("SUBMIT".equals(action)) {
                if (!Set.of("DRAFT", "REJECTED").contains(t.getPayrollApprovalStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT,"Only draft or rejected work can be submitted");
                t.setPayrollApprovalStatus("SUBMITTED"); t.setPayrollSubmittedBy(actor.getName()); t.setPayrollSubmittedAt(LocalDateTime.now());
                t.setPayrollApprovedBy(null); t.setPayrollApprovedAt(null); t.setPayrollDecisionReason(null);
            } else {
                if (!"SUBMITTED".equals(t.getPayrollApprovalStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT,"Submit current work before approval or rejection");
                if ("REJECT".equals(action) && (reason == null || reason.isBlank())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Give a rejection reason");
                t.setPayrollApprovalStatus("APPROVE".equals(action) ? "APPROVED" : "REJECTED");
                t.setPayrollApprovedBy(actor.getName()); t.setPayrollApprovedAt(LocalDateTime.now()); t.setPayrollDecisionReason(reason);
            }
            t.setPayrollReviewFingerprint(t.payrollFingerprint()); rows.add(t);
        }
        timesheets.saveAll(rows);
        return rows.stream().map(this::row).toList();
    }
    Map<String,Object> row(Timesheet t) {
        Map<String,Object> r = new LinkedHashMap<>();
        r.put("id",t.getId()); r.put("employeeId",t.getEmployee().getId());
        r.put("employeeName",t.getEmployee().getFirstName()+" "+t.getEmployee().getLastName());
        r.put("date",t.getDate()); r.put("project",t.getJob() == null ? "" : t.getJob().getName());
        var payable = ProjectDailyHoursRule.calculate(t.getJob(), t.getRegularHours(), t.getOvertimeHours());
        r.put("enteredNormalHours",t.getRegularHours()); r.put("enteredOvertimeHours",t.getOvertimeHours());
        r.put("normalHours",payable.normalHours()); r.put("overtimeHours",payable.overtimeHours());
        r.put("status",t.getPayrollApprovalStatus() == null ? "DRAFT" : t.getPayrollApprovalStatus());
        r.put("approvedBy",t.getPayrollApprovedBy()); r.put("approvedAt",t.getPayrollApprovedAt());
        r.put("reason",t.getPayrollDecisionReason());
        return r;
    }
    @Transactional(readOnly=true)
    public Map<String,Object> list(LocalDate start, LocalDate end, Long jobId) {
        if(start == null || end == null || end.isBefore(start)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Valid dates required");
        return Map.of("canApprove",canApprove(),"rows",timesheets.findByDateBetween(start,end).stream()
                .filter(t -> t.getJob()!=null && (jobId==null || jobId.equals(t.getJob().getId()))).map(this::row).toList());
    }
    @Transactional(readOnly=true)
    public Map<String,Object> review(Long periodId, Long employeeId, List<Long> excluded) {
        PayrollPeriod period = periods.findById(periodId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Payroll period not found"));
        var workScope = payrollWorkScopeService == null ? null : payrollWorkScopeService.prepare(period);
        java.util.function.Predicate<Employee> inScope = e -> e != null && Boolean.TRUE.equals(e.getActive()) && e.getCategory()!=null
                && (period.isProjectPayroll() || Objects.equals(e.getCategory().getName(),period.getCategory())) && (employeeId==null || employeeId.equals(e.getId()))
                && (excluded==null || !excluded.contains(e.getId()));
        List<Timesheet> allSheets = timesheets.findByDateBetween(period.getStartDate(),period.getEndDate());
        List<Timesheet> sheets = allSheets.stream()
                .filter(t -> t.getJob()!=null && (!period.isProjectPayroll() || Objects.equals(period.getProjectId(),t.getJob().getId())) && inScope.test(t.getEmployee())).toList();
        List<Attendance> attendances = attendance.findByDateBetween(period.getStartDate(),period.getEndDate()).stream()
                .filter(a -> inScope.test(a.getEmployee())).toList();
        List<Map<String,Object>> rows = new ArrayList<>();
        for(Timesheet t : sheets) {
            Map<String,Object> r = row(t);
            r.put("alreadyAllocated",workScope != null && workScope.reserved(t.getEmployee().getId(),t.getDate()));
            r.put("synced",attendances.stream().anyMatch(a -> a.getDate().equals(t.getDate()) && a.getEmployee().getId().equals(t.getEmployee().getId())
                    && "TIMESHEET_SYNC".equals(a.getMethod()) && t.payrollFingerprint().equals(a.getTimesheetApprovalFingerprint())
                    && Objects.equals(a.approvedWorkSignature(),a.getTimesheetApprovedWork())
                    && (a.getTimesheetId()==null || t.getId().equals(a.getTimesheetId()))));
            rows.add(r);
        }
        java.util.function.Predicate<Attendance> synced = a -> "TIMESHEET_SYNC".equals(a.getMethod()) || a.getTimesheetId()!=null || a.getProjectId()!=null;
        java.util.function.Predicate<Attendance> eligible = a -> !synced.test(a) || allSheets.stream().anyMatch(t -> t.getDate().equals(a.getDate())
                && t.getEmployee().getId().equals(a.getEmployee().getId()) && (a.getTimesheetId()==null || a.getTimesheetId().equals(t.getId()))
                && (t.getJob()==null || (t.isPayrollApproved() && t.payrollFingerprint().equals(a.getTimesheetApprovalFingerprint()))));
        long excludedSyncedAttendance = attendances.stream().filter(synced).filter(eligible.negate()).count();
        double attendanceHours = attendances.stream().filter(a -> workScope==null || !workScope.reserved(a.getEmployee().getId(),a.getDate())).filter(eligible).filter(a -> !synced.test(a) || allSheets.stream().anyMatch(t -> t.getJob()==null
                && t.getDate().equals(a.getDate()) && t.getEmployee().getId().equals(a.getEmployee().getId())
                && (a.getTimesheetId()==null || a.getTimesheetId().equals(t.getId()))))
                .filter(a -> Set.of("Present","Late","Holiday Present","Weekend Present").contains(a.getStatus()==null?"":a.getStatus()))
                .mapToDouble(Attendance::getMinimumHour).sum();
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("rows",rows); result.put("attendanceNormalHours",period.isProjectPayroll()?0:attendanceHours);
        result.put("periodStart",period.getStartDate()); result.put("periodEnd",period.getEndDate());
        result.put("periodType",period.isProjectPayroll()?"PROJECT":"CATEGORY"); result.put("projectName",period.getProjectName());
        result.put("excludedSyncedAttendanceRecords",excludedSyncedAttendance);
        result.put("excludedAlreadyAllocatedWorkDays",workScope==null?0:workScope.excludedDays);
        if(workScope!=null) result.put("eligibleEmployeeIds",workScope.workDates.keySet());
        return result;
    }
}
