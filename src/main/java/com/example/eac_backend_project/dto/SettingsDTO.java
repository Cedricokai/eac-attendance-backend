package com.example.eac_backend_project.dto;

import java.util.List;

public class SettingsDTO {
    private Double hourlyRate;
    private Double overtimeHourlyRate;
    private Double weekendRate;
    private Double holidayRate;
    private List<Integer> weekendDays;
    private List<HolidayDTO> holidays;
    private Double standardWorkHours;

    // Constructors, getters, and setters
    public SettingsDTO() {}

    public SettingsDTO(Double hourlyRate, Double overtimeHourlyRate, Double weekendRate,
                       Double holidayRate, List<Integer> weekendDays, List<HolidayDTO> holidays,
                       Double standardWorkHours ){
        this.hourlyRate = hourlyRate;
        this.overtimeHourlyRate = overtimeHourlyRate;
        this.weekendRate = weekendRate;
        this.holidayRate = holidayRate;
        this.weekendDays = weekendDays;
        this.holidays = holidays;
        this.standardWorkHours = standardWorkHours;
    }

    // Getters and setters...
    public Double getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(Double hourlyRate) { this.hourlyRate = hourlyRate; }

    public Double getOvertimeHourlyRate() { return overtimeHourlyRate; }
    public void setOvertimeHourlyRate(Double overtimeHourlyRate) { this.overtimeHourlyRate = overtimeHourlyRate; }

    public Double getWeekendRate() { return weekendRate; }
    public void setWeekendRate(Double weekendRate) { this.weekendRate = weekendRate; }

    public Double getHolidayRate() { return holidayRate; }
    public void setHolidayRate(Double holidayRate) { this.holidayRate = holidayRate; }

    public List<Integer> getWeekendDays() { return weekendDays; }
    public void setWeekendDays(List<Integer> weekendDays) { this.weekendDays = weekendDays; }

    public List<HolidayDTO> getHolidays() { return holidays; }
    public void setHolidays(List<HolidayDTO> holidays) { this.holidays = holidays; }

    public Double getStandardWorkHours() { return standardWorkHours; }
    public void setStandardWorkHours(Double standardWorkHours) { this.standardWorkHours = standardWorkHours; }
}