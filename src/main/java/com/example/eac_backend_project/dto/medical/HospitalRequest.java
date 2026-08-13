package com.example.eac_backend_project.dto.medical;
import jakarta.validation.constraints.*;
public record HospitalRequest(@NotBlank String name, @NotBlank String code, String address,
                              @NotBlank String contactName, @NotBlank String phone, @Email String email) {}
