package com.novacore.banking.application.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateCustomerCommand(
    @NotBlank(message = "Full name is required")
    String fullName,

    @NotBlank(message = "Email is required") @Email(message = "Invalid email format")
    String email,

    @NotBlank(message = "Phone number is required") @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone number must be 10-15 digits and can start with +")
    String phoneNumber
) {}
