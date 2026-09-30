package com.novacore.banking.shared.exception;

import java.util.UUID;

public class CustomerNotFoundException extends RuntimeException {
    public CustomerNotFoundException(UUID id) {
        super("Customer not found with ID: " + id);
    }
}
