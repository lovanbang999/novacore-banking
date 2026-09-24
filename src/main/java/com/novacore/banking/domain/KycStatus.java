package com.novacore.banking.domain;

public enum KycStatus {
    PENDING_KYC,
    VERIFIED,
    REJECTED;

    public boolean isVerified() {
        return this == VERIFIED;
    }
}
