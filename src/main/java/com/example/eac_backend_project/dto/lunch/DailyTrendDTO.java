package com.example.eac_backend_project.dto.lunch;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyTrendDTO {
    private LocalDate date;
    private long assigned;
    private long served;
}