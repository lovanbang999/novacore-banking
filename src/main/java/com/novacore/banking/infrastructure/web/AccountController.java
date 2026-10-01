package com.novacore.banking.infrastructure.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Account Management", description = "APIs for opening and activating banking accounts")
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @Operation(summary = "Open new account", description = "Opens a new bank account associated with an existing customer")
    public ResponseEntity<ApiResponse<AccountResponse>> openAccount(@Valid @RequestBody OpenAccountCommand command) {
        AccountResponse response = accountService.openAccount(command);
        return ApiResponse.success(SuccessCode.ACCOUNT_OPEN_SUCCESS, response);
    }

    @PatchMapping("/{accountNumber}/activate")
    @Operation(summary = "Open new account", description = "Opens a new bank account associated with an existing customer")
    public ResponseEntity<ApiResponse<AccountResponse>> activeAccount(@PathVariable String accountNumber) {
        AccountResponse response = accountService.activeAccount(accountNumber);
        return ApiResponse.success(SuccessCode.ACCOUNT_ACTIVATED, response);
    }

    @GetMapping("/{accountNumber}")
    @Operation(
        summary = "Get account details", 
        description = "Retrieves account status, current balance, active holds, and calculated available balance"
    )
    public ResponseEntity<ApiResponse<AccountResponse>> getAccountDetails(@PathVariable String accountNumber) {
        AccountResponse response = accountService.getAccountDetails(accountNumber);
        return ApiResponse.success(SuccessCode.ACCOUNT_DETAILS_SUCCESS, response);
    }
}
