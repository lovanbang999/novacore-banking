package com.novacore.banking.infrastructure.persistence;

import org.springframework.data.jpa.repository.Query;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import com.novacore.banking.domain.AccountHold;
import com.novacore.banking.domain.HoldStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface AccountHoldingRepository extends JpaRepository<AccountHold, UUID> {
    List<AccountHold> findByAccountIdAndStatus(UUID accountId, HoldStatus status);

    @Query("SELECT COALESCE(SUM(h.amount), 0.0000) FROM AccountHold h WHERE h.accountId = :accountId AND h.status = :status")
    BigDecimal sumAmountByAccountIdAndStatus(@Param("accountId") UUID accountId, @Param("status") HoldStatus status);
}
