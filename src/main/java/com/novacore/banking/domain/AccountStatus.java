package com.novacore.banking.domain;

public enum AccountStatus {
    PENDING_KYC,
    ACTIVE,
    FROZEN,
    CLOSED;

    public boolean canTransitionTo(AccountStatus target) {
        if (target == null || this == target) {
            return false;
        }

        return switch (this) {
            case PENDING_KYC -> target == ACTIVE;
            case ACTIVE -> target == FROZEN || target == CLOSED;
            case FROZEN -> target == ACTIVE || target == CLOSED;
            case CLOSED -> false;
            default -> false;
        };
    }
}
