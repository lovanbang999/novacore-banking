package com.novacore.banking.application.dtos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.novacore.banking.domain.AccountHold;
import com.novacore.banking.domain.HoldStatus;

public record AccountHoldResponse(
    UUID id,
    UUID accountId,
    BigDecimal amount,
    String reason,
    HoldStatus status,
    Instant createAt
) {
    public static AccountHoldResponse fromEntity(AccountHold hold) {
        return new AccountHoldResponse(
            hold.getId(),
            hold.getAccountId(),
            hold.getAmount(),
            hold.getReason(),
            hold.getStatus(),
            hold.getCreatedAt()
        );
    }
}
