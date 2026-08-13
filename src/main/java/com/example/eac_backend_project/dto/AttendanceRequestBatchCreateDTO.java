package com.example.eac_backend_project.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AttendanceRequestBatchCreateDTO(
        @NotEmpty @Size(max = 500) List<@Valid AttendanceRequestCreateDTO> requests
) {}
