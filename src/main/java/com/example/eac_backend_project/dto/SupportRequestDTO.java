package com.example.eac_backend_project.dto;
import jakarta.validation.constraints.*;
public record SupportRequestDTO(@NotBlank @Size(max=150) String name, @NotBlank @Email @Size(max=180) String email, @NotBlank @Pattern(regexp="LOGIN_PROBLEM|LOCKED_ACCOUNT|FORGOTTEN_USERNAME|ACCOUNT_REQUEST|CONNECTION_PROBLEM|OTHER") String category, @NotBlank @Size(min=10,max=1000) String description, @Size(max=40) String page) {}
