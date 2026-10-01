package com.novacore.banking.shared.response;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import com.novacore.banking.shared.i18n.MessageHelper;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    boolean success,
    int status,
    String code,
    String message,
    T data,
    Map<String, String> errors,
    Instant timestamp
) {
    // --- Factory methods received Type-Safe Enum ---
    public static <T> ResponseEntity<ApiResponse<T>> success(SuccessCode successCode, T data) {
        return ResponseEntity.status(successCode.getDefaultStatus())
            .body(new ApiResponse<T>(
                true,
                successCode.getDefaultStatus().value(),
                successCode.getKey(),
                MessageHelper.get(successCode.getKey()),
                data,
                null,
                Instant.now()
            ));
    }

    // --- Support passing dynamic args to message. ( For example: ${arg0}, ${arg1}, ${arg2} ) ---
    public static <T> ResponseEntity<ApiResponse<T>> success(SuccessCode successCode, T data, Object... args) {
        return ResponseEntity.status(successCode.getDefaultStatus())
            .body(new ApiResponse<T>(
                true,
                successCode.getDefaultStatus().value(),
                successCode.getKey(),
                MessageHelper.get(successCode.getKey(), args),
                data,
                null,
                Instant.now()
            ));
    }

    // --- Factory methods for Error with ErrorCode Enum ---
    public static <T> ResponseEntity<ApiResponse<T>> error(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getDefaultStatus())
            .body(new ApiResponse<T>(
                false,
                errorCode.getDefaultStatus().value(),
                errorCode.getKey(),
                MessageHelper.get(errorCode.getKey()),
                null,
                null,
                Instant.now()
            ));
    }

    // --- Factory methods for Error with ErrorCode Enum ---
    public static <T> ResponseEntity<ApiResponse<T>> error(ErrorCode errorCode, Map<String, String> errors) {
        return ResponseEntity.status(errorCode.getDefaultStatus())
            .body(new ApiResponse<T>(
                false,
                errorCode.getDefaultStatus().value(),
                errorCode.getKey(),
                MessageHelper.get(errorCode.getKey()),
                null,
                errors,
                Instant.now()
            ));
    }
}
