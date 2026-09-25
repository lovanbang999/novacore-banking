package com.novacore.banking.domain.exception;

import java.util.UUID;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String message) {
        super(message);
    }

    public AccountNotFoundException(UUID accountId) {
        super("Account not found with ID: " + accountId);
    }

    public static AccountNotFoundException forAccountNumber(String accountNumber) {
        return new AccountNotFoundException("Account not found with account number: " + accountNumber);
    }
}
