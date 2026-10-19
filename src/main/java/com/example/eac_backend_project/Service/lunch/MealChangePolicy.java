package com.example.eac_backend_project.Service.lunch;

import java.time.*;

public record MealChangePolicy(String cutoffTime, int daysBefore, String timezone, String frequency, int monthlyDay) {
    public MealChangePolicy(String cutoffTime, int daysBefore, String timezone) {
        this(cutoffTime, daysBefore, timezone, "DAILY", 1);
    }
    public boolean monthlyWindowOpen(ZonedDateTime now) {
        if (!"MONTHLY".equals(frequency)) return true;
        ZonedDateTime local = now.withZoneSameInstant(ZoneId.of(timezone));
        return local.getDayOfMonth() == Math.min(monthlyDay, local.toLocalDate().lengthOfMonth())
                && local.toLocalTime().isBefore(LocalTime.parse(cutoffTime));
    }
    public ZonedDateTime deadline(LocalDate mealDate) {
        return mealDate.minusDays(daysBefore).atTime(LocalTime.parse(cutoffTime)).atZone(ZoneId.of(timezone));
    }
    public boolean editable(LocalDate mealDate, ZonedDateTime now) {
        return monthlyWindowOpen(now) && !mealDate.isBefore(now.withZoneSameInstant(ZoneId.of(timezone)).toLocalDate())
                && now.isBefore(deadline(mealDate));
    }
}
