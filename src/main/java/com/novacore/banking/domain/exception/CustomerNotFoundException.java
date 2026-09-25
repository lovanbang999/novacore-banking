package com.novacore.banking.domain.exception;

import java.util.UUID;

public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(String message) {
        super(message);
    }

    public CustomerNotFoundException(UUID customerId) {
        super("Customer not found with ID: " + customerId);
    }

    public static CustomerNotFoundException forEmail(String email) {
        return new CustomerNotFoundException("Customer not found with email: " + email);
    }
}
