package com.novacore.banking.infrastructure.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import com.novacore.banking.application.AccountService;
import com.novacore.banking.application.dtos.AccountResponse;
import com.novacore.banking.application.dtos.OpenAccountCommand;
import com.novacore.banking.shared.response.ApiResponse;
import com.novacore.banking.shared.response.SuccessCode;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<ApiResponse<AccountResponse>> openAccount(@Valid @RequestBody OpenAccountCommand command) {
        AccountResponse response = accountService.openAccount(command);
        return ApiResponse.success(SuccessCode.ACCOUNT_OPEN_SUCCESS, response);
    }

    @PatchMapping("/{accountNumber}/activate")
    public ResponseEntity<ApiResponse<AccountResponse>> activeAccount(@PathVariable String accountNumber) {
        AccountResponse response = accountService.activeAccount(accountNumber);
        return ApiResponse.success(SuccessCode.ACCOUNT_ACTIVATED, response);
    }
}
