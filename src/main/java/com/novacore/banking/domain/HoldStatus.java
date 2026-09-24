package com.novacore.banking.domain;

public enum HoldStatus {
    ACTIVE,
    RELEASED,
    CAPTURED;

    public boolean isActive() {
        return this == ACTIVE;
    }
}
