package com.novacore.banking.application;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.springframework.stereotype.Service;
import com.novacore.banking.application.dtos.CreateCustomerCommand;
import com.novacore.banking.application.dtos.CustomerResponse;
import com.novacore.banking.application.dtos.UpdateKycStatusCommand;
import com.novacore.banking.domain.Customer;
import com.novacore.banking.domain.KycStatus;
import com.novacore.banking.infrastructure.persistence.CustomerRepository;
import com.novacore.banking.shared.exception.CustomerNotFoundException;

@Service
@RequiredArgsConstructor 
public class CustomerService {
    private final CustomerRepository customerRepository;

    public CustomerResponse registerCustomer(CreateCustomerCommand command) {
        if (customerRepository.existsByEmail(command.email())) {
            throw new IllegalArgumentException("Email already registered: " + command.email());
        }

        if (customerRepository.existsByPhoneNumber(command.phoneNumber())) {
            throw new IllegalArgumentException("Phone number already registered: " + command.phoneNumber());
        }

        Customer customer = Customer.builder()
            .fullname(command.fullName())
            .email(command.email())
            .phoneNumber(command.phoneNumber())
            .kycStatus(KycStatus.PENDING_KYC)
            .build();

        Customer savedCustomer = customerRepository.save(customer);
        return CustomerResponse.fromEntity(savedCustomer);
    }

    public CustomerResponse updateKycStatus(UUID id, UpdateKycStatusCommand command) {
        Customer customer = customerRepository.findById(id).orElseThrow(() -> new CustomerNotFoundException(id));

        switch (command.status()) {
            case VERIFIED:
                customer.verifyKyc();
                break;
            case REJECTED:
                customer.rejectKyc();
                break;
            default:
                throw new IllegalArgumentException("Invalid status transition");
        }

        Customer savedCustomer = customerRepository.save(customer);
        return CustomerResponse.fromEntity(savedCustomer);
    }
}
