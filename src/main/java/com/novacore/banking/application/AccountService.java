package com.novacore.banking.application;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;
import com.novacore.banking.application.dtos.AccountResponse;
import com.novacore.banking.application.dtos.OpenAccountCommand;
import com.novacore.banking.domain.Account;
import com.novacore.banking.domain.AccountStatus;
import com.novacore.banking.domain.Customer;
import com.novacore.banking.domain.HoldStatus;
import com.novacore.banking.domain.KycStatus;
import com.novacore.banking.domain.exception.AccountNotFoundException;
import com.novacore.banking.domain.exception.KycNotVerifiedException;
import com.novacore.banking.infrastructure.persistence.AccountHoldingRepository;
import com.novacore.banking.infrastructure.persistence.AccountRepository;
import com.novacore.banking.infrastructure.persistence.CustomerRepository;
import com.novacore.banking.domain.exception.CustomerNotFoundException;

@Service
@AllArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final AccountHoldingRepository accountHoldingRepository;
    
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

    public AccountResponse activeAccount(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new AccountNotFoundException(accountNumber));
        
        Customer customer = customerRepository.findById(account.getCustomerId())
            .orElseThrow(() -> new CustomerNotFoundException(account.getCustomerId()));
        
        if (customer.getKycStatus() != KycStatus.VERIFIED) {
            throw new KycNotVerifiedException(customer.getId(), customer.getKycStatus());
        }

        account.activate();

        Account savedAccount = accountRepository.save(account);
        return AccountResponse.fromEntity(savedAccount, savedAccount.getCurrentBalance());
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountDetails(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber).orElseThrow(() -> new AccountNotFoundException(accountNumber));

        BigDecimal activeHolds = accountHoldingRepository.sumAmountByAccountIdAndStatus(account.getId(), HoldStatus.ACTIVE);

        if (activeHolds == null) {
            activeHolds = BigDecimal.ZERO.setScale(4);
        }

        BigDecimal availableBalance = account.getCurrentBalance().subtract(activeHolds);

        return AccountResponse.fromEntity(account, availableBalance, activeHolds);
    }

    // Private method
    private String generateUniqueAccountNumber(String currency) {
        String accNo;

        do {
            int randomNum = ThreadLocalRandom.current().nextInt(10000000, 99999999);
            accNo = "ACC-" + currency + "-" + randomNum;
        } while (accountRepository.existsByAccountNumber(accNo));

        return accNo;
    }
}
