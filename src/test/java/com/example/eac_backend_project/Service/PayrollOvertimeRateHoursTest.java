package com.example.eac_backend_project.Service;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class PayrollOvertimeRateHoursTest {
    final LocalDate start = LocalDate.of(2026, 9, 22);

    @Test void includesStartAndEndAndReturnsToOldRateAfterEnd() {
        var split = PayrollOvertimeRateHours.split(Map.of(
                start.minusDays(1), 2.0, start, 3.0, start.plusDays(1), 4.0, start.plusDays(2), 5.0),
                Map.of(), 0, 8, false, date -> false, 1, start, start.plusDays(1));
        assertEquals(7, split.oldRate()); assertEquals(7, split.newRate());
    }

    @Test void automaticWeekendHoursUseMultiplierWithoutDoubleCountingApprovedHours() {
        var date = start.plusDays(4);
        var split = PayrollOvertimeRateHours.split(Map.of(date, 2.0), Map.of(date, 8.0),
                0, 8, true, day -> true, 1.5, start, null);
        assertEquals(0, split.oldRate()); assertEquals(11, split.newRate());
    }

    @Test void excessAttendanceUsesChronologicalPeriodCapAndDailyCap() {
        var split = PayrollOvertimeRateHours.split(Map.of(start.minusDays(1), 2.0),
                Map.of(start.minusDays(1), 12.0, start, 4.0, start.plusDays(1), 8.0),
                10, 8, true, day -> false, 1, start, null);
        assertEquals(2, split.oldRate()); assertEquals(10, split.newRate());
    }

    @Test void noEffectiveRateKeepsAllOvertimeAtOldRate() {
        var split = PayrollOvertimeRateHours.split(Map.of(start, 3.5), Map.of(),
                0, 8, false, day -> false, 1, null, null);
        assertEquals(3.5, split.oldRate()); assertEquals(0, split.newRate());
    }

    @Test void emptyHoursRemainZero() {
        var split = PayrollOvertimeRateHours.split(Map.of(), Map.of(),
                0, 8, false, day -> false, 1, start, null);
        assertEquals(0, split.oldRate()); assertEquals(0, split.newRate());
    }
}
