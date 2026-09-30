package com.novacore.banking.infrastructure.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.novacore.banking.application.AccountService;
import com.novacore.banking.application.dtos.AccountResponse;
import com.novacore.banking.application.dtos.OpenAccountCommand;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponse> openAccount(@Valid @RequestBody OpenAccountCommand command) {
        AccountResponse response = accountService.openAccount(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{accountNumber}/activate")
    public ResponseEntity<AccountResponse> activeAccount(@PathVariable String accountNumber) {
        AccountResponse response = accountService.activeAccount(accountNumber);
        return ResponseEntity.ok(response);
    }
}
