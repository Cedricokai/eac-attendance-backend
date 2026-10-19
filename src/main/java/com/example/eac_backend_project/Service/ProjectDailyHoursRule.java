package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.entity.attendance.Attendance;
import com.example.eac_backend_project.entity.attendance.Job;
import java.util.Set;

/** Optional per-project target for total daily hours; entered timesheets and billing stay intact. */
public final class ProjectDailyHoursRule {
    private ProjectDailyHoursRule() {}
    public record Hours(double normalHours, double overtimeHours) {}
    public static void validate(Job job) {
        if (job == null || !Boolean.TRUE.equals(job.getDailyHoursTopUpEnabled())) return;
        Double threshold = job.getDailyHoursTopUpThreshold(), target = job.getDailyHoursTopUpTarget();
        if (threshold == null || target == null || !Double.isFinite(threshold) || !Double.isFinite(target)
                || threshold < 0 || target <= threshold || target > 24)
            throw new IllegalArgumentException("Daily hours rule requires 0 <= threshold < target <= 24");
        if (Boolean.TRUE.equals(job.getUseConfiguredAttendanceHours()))
            throw new IllegalArgumentException("Disable fixed configured attendance hours before enabling the daily hours top-up rule");
    }
    public static Hours calculate(Job job, double normal, double overtime) {
        if (job == null || !Boolean.TRUE.equals(job.getDailyHoursTopUpEnabled())) return new Hours(normal, overtime);
        validate(job);
        double total = normal + overtime;
        if (!Double.isFinite(total) || normal < 0 || overtime < 0 || total <= job.getDailyHoursTopUpThreshold()) return new Hours(normal, overtime);
        if (total >= job.getDailyHoursTopUpTarget()) {
            // Remove overtime first, then cap normal hours if they alone exceed the target.
            double cappedNormal = Math.min(normal, job.getDailyHoursTopUpTarget());
            return new Hours(cappedNormal, job.getDailyHoursTopUpTarget() - cappedNormal);
        }
        double addition = job.getDailyHoursTopUpTarget() - total;
        double standard = job.getStandardWorkHours() == null ? 8 : job.getStandardWorkHours();
        double normalAddition = Math.min(addition, Math.max(0, standard - normal));
        return new Hours(normal + normalAddition, overtime + addition - normalAddition);
    }
    public static void apply(Attendance row, Job job) {
        if (row == null || row.getStatus() == null || !Set.of("Present", "Late", "Half Day", "Weekend Present", "Special Weekend Present", "Holiday Present").contains(row.getStatus())) return;
        // Never top up an incomplete punch.
        if ((row.getCheckIn() == null) != (row.getCheckOut() == null)) return;
        Hours result = calculate(job, row.getMinimumHour(), row.getOvertime() == null ? 0 : row.getOvertime());
        row.setMinimumHour(result.normalHours()); row.setOvertime(result.overtimeHours());
    }
}
