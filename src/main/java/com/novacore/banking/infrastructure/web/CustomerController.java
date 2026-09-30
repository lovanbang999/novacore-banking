package com.novacore.banking.infrastructure.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import com.novacore.banking.application.dtos.*;
import com.novacore.banking.application.CustomerService;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {
    private final CustomerService customerService;

    @PostMapping 
    public ResponseEntity<CustomerResponse> registerCustomer(@Valid @RequestBody CreateCustomerCommand command) {
        CustomerResponse response = customerService.registerCustomer(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/kyc")
    public ResponseEntity<CustomerResponse> updateKycStatus(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateKycStatusCommand command
    ) {
        CustomerResponse response = customerService.updateKycStatus(id, command);
        return ResponseEntity.ok(response);
    }
}
