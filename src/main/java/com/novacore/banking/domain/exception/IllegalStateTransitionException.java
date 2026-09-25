package com.novacore.banking.domain.exception;

import com.novacore.banking.domain.AccountStatus;

public class IllegalStateTransitionException extends RuntimeException {

    public IllegalStateTransitionException(String message) {
        super(message);
    }

    public IllegalStateTransitionException(AccountStatus from, AccountStatus to) {
        super(String.format("Illegal state transition from %s to %s", from, to));
    }
}
