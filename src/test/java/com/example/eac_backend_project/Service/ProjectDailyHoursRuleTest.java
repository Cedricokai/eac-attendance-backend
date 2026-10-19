package com.example.eac_backend_project.Service;
import com.example.eac_backend_project.entity.attendance.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ProjectDailyHoursRuleTest {
    Job configured() { Job j = new Job(); j.setDailyHoursTopUpEnabled(true); j.setDailyHoursTopUpThreshold(9.3); j.setDailyHoursTopUpTarget(10.0); j.setStandardWorkHours(8); return j; }
    @Test void strictBoundaryAndTargetCap() {
        Job j = configured();
        for(double total : new double[]{0,4,9.2,9.3,10}) {
            var h = ProjectDailyHoursRule.calculate(j, Math.min(total,8),Math.max(0,total-8));
            assertEquals(total,h.normalHours()+h.overtimeHours(),0.000001);
        }
        var h = ProjectDailyHoursRule.calculate(j,8,1.31);
        assertEquals(8,h.normalHours()); assertEquals(2,h.overtimeHours(),0.000001);
    }
    @Test void fillsNormalHoursBeforeAddingOvertime() {
        Job j=configured(); j.setStandardWorkHours(10);
        var h=ProjectDailyHoursRule.calculate(j,9.5,0);
        assertEquals(10,h.normalHours()); assertEquals(0,h.overtimeHours());
    }
    @Test void disabledAndOtherProjectsKeepEnteredHours() {
        var h=ProjectDailyHoursRule.calculate(new Job(),8,1.5); assertEquals(1.5,h.overtimeHours());
        h=ProjectDailyHoursRule.calculate(null,8,1.5); assertEquals(1.5,h.overtimeHours());
    }
    @Test void rejectsInvalidAndConflictingConfiguration() {
        Job j=configured(); j.setDailyHoursTopUpTarget(9.3); assertThrows(IllegalArgumentException.class,()->ProjectDailyHoursRule.validate(j));
        j.setDailyHoursTopUpTarget(Double.NaN); assertThrows(IllegalArgumentException.class,()->ProjectDailyHoursRule.validate(j));
        j.setDailyHoursTopUpTarget(10.0); j.setUseConfiguredAttendanceHours(true); assertThrows(IllegalArgumentException.class,()->ProjectDailyHoursRule.validate(j));
    }
    @Test void absenceAndIncompletePunchesAreNotCredited() {
        Attendance a=new Attendance(); a.setMinimumHour(8); a.setOvertime(1.5); a.setStatus("Absent");
        ProjectDailyHoursRule.apply(a,configured()); assertEquals(1.5,a.getOvertime());
        a.setStatus("Present"); a.setCheckIn(java.time.LocalTime.of(7,0));
        ProjectDailyHoursRule.apply(a,configured()); assertEquals(1.5,a.getOvertime());
    }
    @Test void repeatApplicationDoesNotIncreaseHoursAgain() {
        Attendance a=new Attendance(); a.setStatus("Present"); a.setMinimumHour(8); a.setOvertime(1.5);
        ProjectDailyHoursRule.apply(a,configured()); ProjectDailyHoursRule.apply(a,configured()); assertEquals(2,a.getOvertime());
    }
    @Test void changedRuleInvalidatesApproval() {
        Timesheet t=new Timesheet(); t.setJob(configured()); t.setRegularHours(8); t.setOvertimeHours(1.5);
        String old=t.payrollFingerprint(); t.getJob().setDailyHoursTopUpThreshold(9.4); assertNotEquals(old,t.payrollFingerprint());
    }
    @Test void totalsAboveTargetBecomeExactlyTargetWithoutNegativeHours() {
        Job j=configured();
        for (double[] pair : new double[][]{{8,3},{8,4},{11,0},{12,4},{4,10}}) {
            var h=ProjectDailyHoursRule.calculate(j,pair[0],pair[1]);
            assertEquals(10,h.normalHours()+h.overtimeHours(),0.000001);
            assertTrue(h.normalHours()>=0); assertTrue(h.overtimeHours()>=0);
            assertEquals(Math.min(pair[0],10),h.normalHours());
            var again=ProjectDailyHoursRule.calculate(j,h.normalHours(),h.overtimeHours());
            assertEquals(h,again);
        }
    }
}
