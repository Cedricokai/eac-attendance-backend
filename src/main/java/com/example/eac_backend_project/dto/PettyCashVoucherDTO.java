package com.example.eac_backend_project.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
public record PettyCashVoucherDTO(@NotBlank @Size(max=150) String paidTo,@NotBlank @Size(max=50) String paymentMethod,@Size(max=120) String paymentReference,@Size(max=500) String receiptReference,@Size(max=255) String receiptFileName,@Size(max=1000) String notes,@NotEmpty List<@Valid PettyCashItemDTO> items) {}
