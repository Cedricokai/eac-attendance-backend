package com.example.eac_backend_project.Service;
import com.example.eac_backend_project.entity.attendance.*;
import com.example.eac_backend_project.entity.settings.Category;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.*;

class JobScheduleHoursTest {
    private void check(String start, String in, String out, double normal, double overtime) {
        var result = JobScheduleHours.calculate(LocalTime.parse(start), 8, LocalTime.parse(in), LocalTime.parse(out));
        assertEquals(normal, result.normalHours(), 0.000001);
        assertEquals(overtime, result.overtimeHours(), 0.000001);
    }
    @Test void lateArrivalPreservesWorkedOvertime() { check("07:00", "08:00", "17:00", 7, 2); }
    @Test void onTime() { check("07:00", "07:00", "17:00", 8, 2); }
    @Test void earlyArrivalIsNotExtraTime() { check("07:00", "06:00", "17:00", 8, 2); }
    @Test void earlyDepartureDoesNotAwardExpectedOvertime() { check("07:00", "08:00", "14:00", 6, 0); }
    @Test void overtimeOnly() { check("07:00", "16:00", "17:00", 0, 1); }
    @Test void boundary() { check("07:00", "08:00", "15:00", 7, 0); }
    @Test void fractionalHours() { check("07:00", "08:30", "16:30", 6.5, 1.5); }
    @Test void overnight() { check("22:00", "23:00", "08:00", 7, 2); }
    @Test void postMidnightArrival() { check("22:00", "01:00", "08:00", 5, 2); }
    @Test void jobOverridesCategoryInBothAttendanceCalculators() {
        Job job = new Job(); job.setWorkStartTime(LocalTime.of(7,0)); job.setStandardWorkHours(8);
        Category category = new Category(); category.setWorkStartTime("09:00"); category.setStandardRateHours(6);
        Employee employee = new Employee(); employee.setJob(job); employee.setCategory(category);
        Attendance attendance = new Attendance(); attendance.setEmployee(employee);
        attendance.setCheckIn(LocalTime.of(8,0)); attendance.setCheckOut(LocalTime.of(17,0));
        AttendanceService service = new AttendanceService(null, null, null, null);
        for (String method : new String[]{"calculateAndSetMinimumHours", "calculateWorkingHours"}) {
            ReflectionTestUtils.invokeMethod(service, method, attendance);
            assertEquals(7, attendance.getMinimumHour(), 0.000001);
            assertEquals(2, attendance.getOvertime(), 0.000001);
            assertEquals(8, attendance.getStandardHours(), 0.000001);
        }
        assertEquals("07:00", service.getCategoryWorkStartTime(employee));
    }
}
