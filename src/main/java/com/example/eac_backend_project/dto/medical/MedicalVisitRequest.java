package com.example.eac_backend_project.dto.medical;
import com.example.eac_backend_project.entity.medical.MedicalVisit;
import jakarta.validation.constraints.*;
import java.time.*;
public record MedicalVisitRequest(@NotNull Long employeeId, @NotNull LocalDate visitDate, LocalTime arrivalTime,
 LocalTime dischargeTime, @NotNull MedicalVisit.VisitType visitType, boolean requiresExcuseDuty,
 LocalDate excuseStartDate, LocalDate excuseEndDate, boolean fitToReturn, LocalDate followUpDate,
 String hospitalReference, String remarks, @NotBlank String submittedByName, @NotBlank String submittedByContact) {}
