package com.example.eac_backend_project.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record AttendanceRequestCreateDTO(
        @NotNull Long employeeId,
        @NotNull LocalDate date,
        @NotNull @JsonFormat(pattern = "HH:mm:ss") LocalTime checkIn,
        @NotNull @JsonFormat(pattern = "HH:mm:ss") LocalTime checkOut,
        @NotBlank @Size(max = 40) String shift,
        @NotBlank @Size(max = 1000) String reason,
        @Size(max = 255) String evidenceReference
) {}
