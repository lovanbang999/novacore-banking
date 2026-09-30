package com.novacore.banking.application.dtos;

import java.time.Instant;
import java.util.UUID;
import com.novacore.banking.domain.Customer;
import com.novacore.banking.domain.KycStatus;

public record CustomerResponse(
    UUID id,
    String fullName,
    String email,
    String phoneNumber,
    KycStatus kycStatus,
    Instant createdAt
) {
    public static CustomerResponse fromEntity(Customer customer) {
        return new CustomerResponse(
            customer.getId(),
            customer.getFullname(),
            customer.getEmail(),
            customer.getPhoneNumber(),
            customer.getKycStatus(),
            customer.getCreatedAt()
        );
    }

}
