package com.example.eac_backend_project.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
public record PettyCashRequestDTO(@NotBlank @Size(max=100) String category,@NotBlank @Size(min=5,max=1000) String purpose,@Size(max=120) String costCenter,@NotEmpty List<@Valid PettyCashItemDTO> items) {}
