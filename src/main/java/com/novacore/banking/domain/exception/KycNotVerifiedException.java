package com.novacore.banking.domain.exception;

import com.novacore.banking.domain.KycStatus;

import java.util.UUID;

public class KycNotVerifiedException extends RuntimeException {

    public KycNotVerifiedException(String message) {
        super(message);
    }

    public KycNotVerifiedException(UUID customerId, KycStatus status) {
        super(String.format("Customer %s KYC verification failed: current status is %s, required VERIFIED", customerId, status));
    }
}
