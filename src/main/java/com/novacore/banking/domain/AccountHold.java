package com.novacore.banking.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "account_holds")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor

public class AccountHold {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal amount;

    @Column(name = "reason")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private HoldStatus status = HoldStatus.ACTIVE;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Domain lifecycle methods
    public void release() {
        if (this.status != HoldStatus.ACTIVE) {
            throw new IllegalStateException("Only ACTIVE holds can be released.");
        }

        this.status = HoldStatus.RELEASED;
    }

    public void capture() {
        if (this.status != HoldStatus.ACTIVE) {
            throw new IllegalStateException("Only ACTIVE holds can be captured.");
        }

        this.status = HoldStatus.CAPTURED;
    }
}
