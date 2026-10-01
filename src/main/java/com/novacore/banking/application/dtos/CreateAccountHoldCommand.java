package com.novacore.banking.application.dtos;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAccountHoldCommand(
    @NotNull(message = "Hold amount is required")
    @DecimalMin(value = "0.01", message = "Hold amount must be strictly positive")
    BigDecimal amount,

    @NotBlank(message = "Hold reason is required")
    @Size(max = 255, message = "Reason cannot exceed 255 characters")
    String reason
) {}
