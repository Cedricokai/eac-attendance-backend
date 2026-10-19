package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.entity.attendance.*;
import com.example.eac_backend_project.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.*;

/** Selects actual work for project/category payroll and excludes work reserved by another run. */
@Service
public class PayrollWorkScopeService {
    private final AttendanceRepository attendance;
    private final TimesheetRepository timesheets;
    private final OvertimeRepository overtime;
    private final PayrollRecordRepository records;
    private final ObjectMapper mapper;
    public PayrollWorkScopeService(AttendanceRepository attendance, TimesheetRepository timesheets,
            OvertimeRepository overtime, PayrollRecordRepository records, ObjectMapper mapper) {
        this.attendance=attendance; this.timesheets=timesheets; this.overtime=overtime; this.records=records; this.mapper=mapper;
    }
    public static final class Scope {
        public final Map<Long,Map<LocalDate,Double>> daily = new HashMap<>();
        public final Map<Long,List<LocalDate>> dates = new HashMap<>();
        public final Map<Long,Set<LocalDate>> workDates = new HashMap<>();
        public final Map<Long,Set<LocalDate>> reserved = new HashMap<>();
        public final List<Overtime> approvedOvertime = new ArrayList<>();
        public int excludedDays;
        public boolean reserved(Long employee, LocalDate date) { return reserved.getOrDefault(employee,Set.of()).contains(date); }
        public void claim(Long employee, LocalDate date) { workDates.computeIfAbsent(employee,k->new TreeSet<>()).add(date); }
        public List<Object[]> normalSummary() {
            List<Object[]> result=new ArrayList<>();
            daily.forEach((id,days)->result.add(new Object[]{id,(long)days.size(),days.values().stream().mapToDouble(Double::doubleValue).sum()}));
            return result;
        }
        public List<Object[]> overtimeSummary() {
            Map<String,Object[]> groups=new LinkedHashMap<>();
            for(Overtime o:approvedOvertime) {
                String key=o.getEmployee().getId()+":"+Boolean.TRUE.equals(o.getMultiplierAppliedToHours());
                Object[] row=groups.computeIfAbsent(key,k->new Object[]{o.getEmployee().getId(),0.0,0.0,Boolean.TRUE.equals(o.getMultiplierAppliedToHours())});
                row[1]=(double)row[1]+(o.getOvertimeHours()==null?0:o.getOvertimeHours());
                row[2]=(double)row[2]+(o.getCalculatedOvertimePay()==null?0:o.getCalculatedOvertimePay());
            }
            return new ArrayList<>(groups.values());
        }
    }
    public Scope prepare(PayrollPeriod period) {
        Scope result=new Scope();
        for(PayrollRecord r:records.findWorkReservations(period.getId(),period.getStartDate(),period.getEndDate())) {
            Set<LocalDate> dates=result.reserved.computeIfAbsent(r.getEmployee().getId(),k->new HashSet<>());
            if(r.getPayrollWorkDates()!=null) {
                try { for(String date:mapper.readValue(r.getPayrollWorkDates(),String[].class)) dates.add(LocalDate.parse(date)); }
                catch(Exception e) { throw new IllegalStateException("Invalid saved payroll work dates for record "+r.getId(),e); }
            } else {
                // Old payrolls have no immutable date list. Conservatively protect their entire period.
                LocalDate start=r.getPeriod().getStartDate().isBefore(period.getStartDate())?period.getStartDate():r.getPeriod().getStartDate();
                LocalDate end=r.getPeriod().getEndDate().isAfter(period.getEndDate())?period.getEndDate():r.getPeriod().getEndDate();
                for(LocalDate d=start;!d.isAfter(end);d=d.plusDays(1)) dates.add(d);
            }
        }
        List<Timesheet> sheets=timesheets.findByDateBetween(period.getStartDate(),period.getEndDate());
        Map<Long,Set<LocalDate>> projectDates=new HashMap<>();
        Set<String> excluded=new HashSet<>();
        Set<String> staleSynced=new HashSet<>();
        for(Attendance a:attendance.findByDateBetween(period.getStartDate(),period.getEndDate())) {
            Employee e=a.getEmployee(); if(e==null || !Boolean.TRUE.equals(e.getActive())) continue;
            boolean synced="TIMESHEET_SYNC".equals(a.getMethod()) || a.getTimesheetId()!=null || a.getProjectId()!=null;
            Timesheet sheet=sheets.stream().filter(t->t.getEmployee()!=null && Objects.equals(t.getEmployee().getId(),e.getId())
                    && Objects.equals(t.getDate(),a.getDate()) && (a.getTimesheetId()==null || Objects.equals(t.getId(),a.getTimesheetId()))).findFirst().orElse(null);
            if(period.isProjectPayroll()) {
                if(!synced || sheet==null || sheet.getJob()==null || !Objects.equals(period.getProjectId(),sheet.getJob().getId())) continue;
            } else if(e.getCategory()==null || !Objects.equals(period.getCategory(),e.getCategory().getName())) continue;
            if(synced && (sheet==null || (sheet.getJob()!=null && (!sheet.isPayrollApproved()
                    || !sheet.payrollFingerprint().equals(a.getTimesheetApprovalFingerprint())
                    || !Objects.equals(a.approvedWorkSignature(),a.getTimesheetApprovedWork()))))) {
                staleSynced.add(e.getId()+":"+a.getDate()); continue;
            }
            if(result.reserved(e.getId(),a.getDate())) { excluded.add(e.getId()+":"+a.getDate()); continue; }
            if(!Set.of("Present","Late","Overtime","Half Day","Weekend Present","Special Weekend Present","Holiday Present","Holiday").contains(a.getStatus()==null?"":a.getStatus())) continue;
            projectDates.computeIfAbsent(e.getId(),k->new HashSet<>()).add(a.getDate());
            if(a.getMinimumHour()>0) {
                result.daily.computeIfAbsent(e.getId(),k->new HashMap<>()).merge(a.getDate(),a.getMinimumHour(),Double::sum);
                result.dates.computeIfAbsent(e.getId(),k->new ArrayList<>());
                if(!result.dates.get(e.getId()).contains(a.getDate()))result.dates.get(e.getId()).add(a.getDate());
                result.claim(e.getId(),a.getDate());
            }
        }
        for(Overtime o:overtime.findApprovedOvertimeForPayrollPeriod(period.getStartDate(),period.getEndDate())) {
            Employee e=o.getEmployee(); if(e==null || !Boolean.TRUE.equals(e.getActive()) || o.getDate()==null)continue;
            if(staleSynced.contains(e.getId()+":"+o.getDate()))continue;
            if(period.isProjectPayroll()) {
                if(!projectDates.getOrDefault(e.getId(),Set.of()).contains(o.getDate()))continue;
            } else if(e.getCategory()==null || !Objects.equals(period.getCategory(),e.getCategory().getName()))continue;
            if(result.reserved(e.getId(),o.getDate())) {excluded.add(e.getId()+":"+o.getDate());continue;}
            if(o.getOvertimeHours()!=null && o.getOvertimeHours()>0) {result.approvedOvertime.add(o);result.claim(e.getId(),o.getDate());}
        }
        result.excludedDays=excluded.size();
        return result;
    }
    public String snapshot(Set<LocalDate> dates) {
        try {return mapper.writeValueAsString(dates.stream().sorted().map(LocalDate::toString).toList());}
        catch(Exception e){throw new IllegalStateException("Cannot save payroll work dates",e);}
    }
}
