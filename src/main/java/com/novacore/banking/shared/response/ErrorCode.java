package com.novacore.banking.shared.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    CUSTOMER_NOT_FOUND("error.customer.not_found", HttpStatus.NOT_FOUND),
    ACCOUNT_NOT_FOUND("error.account.not_found", HttpStatus.NOT_FOUND),
    KYC_NOT_VERIFIED("error.kyc.not_verified", HttpStatus.UNPROCESSABLE_CONTENT),
    ILLEGAL_STATE("error.illegal.state", HttpStatus.UNPROCESSABLE_CONTENT),
    VALIDATION_FAILED("error.validation.failed", HttpStatus.BAD_REQUEST),
    INVALID_ARGUMENT("error.invalid.argument", HttpStatus.BAD_REQUEST);

    private final String key;
    private final HttpStatus defaultStatus;
}
