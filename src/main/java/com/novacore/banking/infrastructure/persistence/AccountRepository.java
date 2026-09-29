package com.novacore.banking.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import com.novacore.banking.domain.Account;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existByAccountNumber(String accountNumber);

    List<Account> findByCustomerId(UUID customerId);
}
