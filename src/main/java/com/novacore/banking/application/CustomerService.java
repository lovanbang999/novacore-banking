package com.novacore.banking.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.novacore.banking.application.dtos.CreateCustomerCommand;
import com.novacore.banking.application.dtos.CustomerResponse;
import com.novacore.banking.domain.Customer;
import com.novacore.banking.domain.KycStatus;
import com.novacore.banking.infrastructure.persistence.CustomerRepository;

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
}
