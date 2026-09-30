package com.novacore.banking.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import com.novacore.banking.domain.Customer;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);
}
