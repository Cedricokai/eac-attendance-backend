package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.entity.attendance.Attendance;
import com.example.eac_backend_project.entity.settings.Holiday;
import com.example.eac_backend_project.repository.AttendanceRepository;
import com.example.eac_backend_project.repository.EmployeeRepository;
import com.example.eac_backend_project.repository.settings.HolidayRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.ArrayList;

@Service
public class HolidayAttendanceService {
    private static final ZoneId ZONE = ZoneId.of("Africa/Accra");
    private final HolidayRepository holidays;
    private final EmployeeRepository employees;
    private final AttendanceRepository attendance;
    public HolidayAttendanceService(HolidayRepository holidays, EmployeeRepository employees, AttendanceRepository attendance) {
        this.holidays = holidays; this.employees = employees; this.attendance = attendance;
    }

    // Independent of absence/report switches. Rechecking also recovers after a short outage.
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void fillCurrentHolidays() {
        LocalDate today = LocalDate.now(ZONE);
        fillDate(today.minusDays(1), today);
        fillDate(today, today);
    }

    @Transactional
    public void holidaySaved(Holiday holiday) {
        LocalDate today = LocalDate.now(ZONE);
        LocalDate date = holiday.getDate();
        if (date == null) return;
        if (holiday.isRecurring()) {
            MonthDay day = MonthDay.from(date);
            if (!day.isValidYear(today.getYear())) return;
            date = day.atYear(today.getYear());
        }
        fillDate(date, today);
    }

    void fillDate(LocalDate date, LocalDate today) {
        if (date.isAfter(today)) return;
        // Serialize repeated runs for the same holiday, including across server instances.
        if (holidays.lockHolidaysOn(date).isEmpty()) return;
        var records = new ArrayList<Attendance>();
        for (var employee : employees.findEligibleForHoliday(date)) {
            var record = new Attendance();
            record.setEmployee(employee);
            record.setDate(date);
            record.setStatus("Holiday");
            record.setMinimumHour(8.0);
            record.setStandardHours(8.0);
            record.setOvertime(0.0);
            record.setBiometric(false);
            record.setMethod("AUTO_HOLIDAY");
            record.setWorkType("Regular");
            record.setShift(employee.getCategory() != null && Boolean.TRUE.equals(employee.getCategory().getOvernightShift()) ? "Night" : "Day");
            records.add(record);
        }
        if (!records.isEmpty()) attendance.saveAll(records);
    }
}
