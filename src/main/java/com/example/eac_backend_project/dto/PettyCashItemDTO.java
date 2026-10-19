package com.example.eac_backend_project.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record PettyCashItemDTO(@NotBlank @Size(max=300) String description,@NotNull @DecimalMin("0.01") BigDecimal quantity,@NotNull @DecimalMin("0.00") BigDecimal unitCost) {}
