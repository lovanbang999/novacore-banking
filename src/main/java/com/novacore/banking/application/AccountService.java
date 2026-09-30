package com.novacore.banking.application;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;
import com.novacore.banking.application.dtos.AccountResponse;
import com.novacore.banking.application.dtos.OpenAccountCommand;
import com.novacore.banking.domain.Account;
import com.novacore.banking.domain.AccountStatus;
import com.novacore.banking.infrastructure.persistence.AccountRepository;
import com.novacore.banking.infrastructure.persistence.CustomerRepository;
import com.novacore.banking.shared.exception.CustomerNotFoundException;

@Service
@AllArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    
    public AccountResponse openAccount(OpenAccountCommand command) {
        if (!customerRepository.existsById(command.customerId())) {
            throw new CustomerNotFoundException(command.customerId());
        }
        
        String accountNumber = generateUniqueAccountNumber(command.currency()).toUpperCase();

        Account account = Account.builder()
            .customerId(command.customerId())
            .accountNumber(accountNumber)
            .currency(command.currency().toUpperCase())
            .status(AccountStatus.PENDING_KYC)
            .currentBalance(BigDecimal.ZERO.setScale(4))
            .build();

        Account savedAccount = accountRepository.save(account);
        return AccountResponse.fromEntity(savedAccount, BigDecimal.ZERO.setScale(4));
    }

    private String generateUniqueAccountNumber(String currency) {
        String accNo;

        do {
            int randomNum = ThreadLocalRandom.current().nextInt(10000000, 99999999);
            accNo = "ACC-" + currency + "-" + randomNum;
        } while (accountRepository.existsByAccountNumber(accNo));

        return accNo;
    }
}
