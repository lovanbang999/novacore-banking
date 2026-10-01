package com.novacore.banking.shared.exception;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import com.novacore.banking.domain.exception.AccountNotFoundException;
import com.novacore.banking.domain.exception.IllegalStateTransitionException;
import com.novacore.banking.domain.exception.KycNotVerifiedException;
import com.novacore.banking.domain.exception.CustomerNotFoundException;
import com.novacore.banking.shared.response.ApiResponse;
import com.novacore.banking.shared.response.ErrorCode;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomerNotFound(CustomerNotFoundException ex) {
        return ApiResponse.error(ErrorCode.CUSTOMER_NOT_FOUND);
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountNotFound(AccountNotFoundException ex) {
        return ApiResponse.error(ErrorCode.ACCOUNT_NOT_FOUND);
    }

    @ExceptionHandler(KycNotVerifiedException.class)
    public ResponseEntity<ApiResponse<Void>> handleKycNotVerified(KycNotVerifiedException ex) {
        return ApiResponse.error(ErrorCode.KYC_NOT_VERIFIED);
    }

    @ExceptionHandler(IllegalStateTransitionException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalStateTransition(IllegalStateTransitionException ex) {
        return ApiResponse.error(ErrorCode.ILLEGAL_STATE);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex) {
        return ApiResponse.error(ErrorCode.ILLEGAL_STATE);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ApiResponse.error(ErrorCode.INVALID_ARGUMENT);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        return ApiResponse.error(ErrorCode.VALIDATION_FAILED, errors);
    }
}
