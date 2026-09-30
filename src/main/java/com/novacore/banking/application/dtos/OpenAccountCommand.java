package com.novacore.banking.application.dtos;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record OpenAccountCommand(
    @NotNull(message = "Customer ID is required")
    UUID customerId,

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code (e.g. VND, USD)")
    String currency,

    String accountType
) {}
