package com.example.eac_backend_project.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AttendanceRequestReviewDTO(
        @NotEmpty List<Long> requestIds,
        @Size(max = 1000) String rejectionReason
) {}
