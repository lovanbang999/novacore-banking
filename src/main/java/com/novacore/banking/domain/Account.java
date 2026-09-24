package com.novacore.banking.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "account_number", nullable = false, unique = true, length = 30)
    private String accountNumber;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private AccountStatus status = AccountStatus.PENDING_KYC;

    @Column(name = "current_balance", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal currentBalance = BigDecimal.ZERO.setScale(4);

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Domain Lifecycle State Machine Actions
    public void activate() {
        transitionTo(AccountStatus.ACTIVE, "Cannot activate account from current state: " + this.status);
    }

    public void freeze() {
        transitionTo(AccountStatus.FROZEN, "Cannot freeze account from current state: " + this.status);
    }

    public void close() {
        if (this.currentBalance.compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalStateException("Account balance must be exactly 0.0000 to close.");
        }

        transitionTo(AccountStatus.CLOSED, "Cannot close account from current state: " + this.status);
    }

    private void transitionTo(AccountStatus targetStatus, String errorMessage) {
        if (!this.status.canTransitionTo(targetStatus)) {
            throw new IllegalStateException(errorMessage);
        }

        this.status = targetStatus;
    }

    // Balance Mutation Methods (Called by Transfer and Settlement Engines)
    public void credit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be strictly positive.");
        }

        this.currentBalance = this.currentBalance.add(amount);
    }

    public void debit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debit amount must be strictly positive.");
        }

        if (this.currentBalance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds in current balance.");
        }

        this.currentBalance = this.currentBalance.subtract(amount);
    }
}
