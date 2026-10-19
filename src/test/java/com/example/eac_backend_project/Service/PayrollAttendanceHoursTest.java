package com.example.eac_backend_project.Service;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PayrollAttendanceHoursTest {
    @Test void movesOnlyActualExcessHoursAndAddsToExistingOvertime() {
        var result = PayrollAttendanceHours.split(200, 23, 8, true);
        assertEquals(184, result.normal());
        assertEquals(56, 40 + result.overtime());
        assertEquals(240, result.normal() + 40 + result.overtime());
    }
    @Test void fewerHoursAreNotScaledByAttendanceDays() {
        var result = PayrollAttendanceHours.split(175, 23, 8, true);
        assertEquals(175, result.normal()); assertEquals(0, result.overtime());
    }
    @Test void uncheckedKeepsAllNormalHours() {
        var result = PayrollAttendanceHours.split(200, 23, 8, false);
        assertEquals(200, result.normal()); assertEquals(0, result.overtime());
    }
    @Test void usesEmployeeStandardHoursAndHandlesExactCapAndZero() {
        assertEquals(39, PayrollAttendanceHours.split(200,23,7,true).overtime());
        assertEquals(0, PayrollAttendanceHours.split(184,23,8,true).overtime());
        assertEquals(0, PayrollAttendanceHours.split(0,23,8,true).normal());
    }
    @Test void periodDaysMatchDisplayedWeekdays() {
        assertEquals(23, PayrollAttendanceHours.weekdays(LocalDate.of(2026,7,1),LocalDate.of(2026,7,31)));
    }
}
