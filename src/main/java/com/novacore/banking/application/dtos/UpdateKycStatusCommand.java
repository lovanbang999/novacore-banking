package com.novacore.banking.application.dtos;

import com.novacore.banking.domain.KycStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateKycStatusCommand(
    @NotNull(message = "KYC status is required")
    KycStatus status
) {}
