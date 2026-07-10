package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.dto.SettingsDTO;
import com.example.eac_backend_project.dto.HolidayDTO;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
public class PayrollCalculator {

    /**
     * Calculate the multiplier for a specific date based on holiday/weekend settings
     */
    public double calculateDateMultiplier(LocalDate date, SettingsDTO settings) {
        if (settings == null) {
            return 1.0;
        }

        // Check for holiday first (highest priority)
        if (isHoliday(date, settings.getHolidays())) {
            return settings.getHolidayRate() != null ? settings.getHolidayRate() : 1.5;
        }

        // Check for weekend
        if (isWeekend(date, settings.getWeekendDays())) {
            return settings.getWeekendRate() != null ? settings.getWeekendRate() : 1.25;
        }

        // Regular day
        return 1.0;
    }

    /**
     * Check if date is a holiday
     */
    public boolean isHoliday(LocalDate date, List<HolidayDTO> holidays) {
        if (holidays == null || holidays.isEmpty()) {
            return false;
        }

        for (HolidayDTO holiday : holidays) {
            if (isHolidayMatch(date, holiday)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if date matches a holiday (handles recurring holidays)
     */
    private boolean isHolidayMatch(LocalDate date, HolidayDTO holiday) {
        if (holiday.getDate() == null) {
            return false;
        }

        if (Boolean.TRUE.equals(holiday.getRecurring())) {
            // For recurring holidays, compare month and day only
            return holiday.getDate().getMonth() == date.getMonth() &&
                    holiday.getDate().getDayOfMonth() == date.getDayOfMonth();
        } else {
            // For non-recurring holidays, exact date match
            return holiday.getDate().equals(date);
        }
    }

    /**
     * Check if date is a weekend based on configured weekend days
     */
    public boolean isWeekend(LocalDate date, List<Integer> weekendDays) {
        if (weekendDays == null || weekendDays.isEmpty()) {
            // Default to Saturday and Sunday
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
        }

        int dayOfWeekValue = date.getDayOfWeek().getValue();
        // Convert to 0-based (Sunday = 0) for consistency with frontend
        int normalizedDay = dayOfWeekValue % 7;
        return weekendDays.contains(normalizedDay);
    }

    /**
     * Calculate daily pay with multipliers
     */
    public double calculateDailyPay(double hoursWorked, double hourlyRate, LocalDate date, SettingsDTO settings) {
        double multiplier = calculateDateMultiplier(date, settings);
        return hoursWorked * hourlyRate * multiplier;
    }

    /**
     * Calculate the bonus amount from multipliers
     */
    public double calculateMultiplierBonus(double basePay, double multiplier) {
        if (multiplier <= 1.0) {
            return 0.0;
        }
        return basePay * (multiplier - 1.0);
    }
}
