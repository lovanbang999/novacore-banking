package com.novacore.banking.application.dtos;

import java.math.BigDecimal;
import java.util.UUID;

import com.novacore.banking.domain.Account;
import com.novacore.banking.domain.AccountStatus;

public record AccountResponse(
    UUID id,
    String accountNumber,
    UUID customerId,
    String currency,
    AccountStatus status,
    BigDecimal currentBalance,
    BigDecimal availableBalance,
    BigDecimal activeHoldsAount
) {
    public static AccountResponse fromEntity(Account account, BigDecimal availableBalance) {
        BigDecimal holds = account.getCurrentBalance().subtract(availableBalance);
        return new AccountResponse(
            account.getId(),
            account.getAccountNumber(),
            account.getCustomerId(),
            account.getCurrency(),
            account.getStatus(),
            account.getCurrentBalance(),
            availableBalance,
            holds
        );
    }

    public static AccountResponse fromEntity(Account account, BigDecimal availableBalance, BigDecimal activeHoldsAmount) {
        return new AccountResponse(
            account.getId(),
            account.getAccountNumber(),
            account.getCustomerId(),
            account.getCurrency(),
            account.getStatus(),
            account.getCurrentBalance(),
            availableBalance,
            activeHoldsAmount
        );
    }
}
