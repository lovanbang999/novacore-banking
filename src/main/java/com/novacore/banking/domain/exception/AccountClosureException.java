package com.novacore.banking.domain.exception;

import java.math.BigDecimal;

public class AccountClosureException extends RuntimeException {

    public AccountClosureException(String message) {
        super(message);
    }

    public static AccountClosureException nonZeroBalance(BigDecimal balance) {
        return new AccountClosureException("Cannot close account with non-zero balance: " + balance);
    }

    public static AccountClosureException activeHoldsExist(int holdCount) {
        return new AccountClosureException(String.format("Cannot close account with %d active hold(s)", holdCount));
    }
}
