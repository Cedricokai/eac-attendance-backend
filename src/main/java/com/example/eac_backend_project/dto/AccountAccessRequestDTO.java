package com.example.eac_backend_project.dto;
import jakarta.validation.constraints.*;
public record AccountAccessRequestDTO(@NotBlank @Size(max=150) String fullName, @NotBlank @Size(max=80) String employeeId, @NotBlank @Size(max=120) String department, @NotBlank @Email @Size(max=180) String workEmail, @NotBlank @Size(max=40) String mobile, @NotBlank @Size(min=10,max=500) String accessReason) {}
