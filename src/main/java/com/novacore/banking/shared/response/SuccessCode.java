package com.novacore.banking.shared.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SuccessCode {
    // -- Customer --
    CUSTOMER_REGISTERED_SUCCESS("customer.register.success", HttpStatus.CREATED),
    CUSTOMER_KYC_UPDATED("customer.kyc.updated", HttpStatus.OK),

    // --- Account ---
    ACCOUNT_OPEN_SUCCESS("account.open.success", HttpStatus.CREATED),
    ACCOUNT_ACTIVATED("account.activate.success", HttpStatus.OK);

    private final String key;
    private final HttpStatus defaultStatus;
}
