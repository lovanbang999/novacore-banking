package com.novacore.banking.infrastructure.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import com.novacore.banking.shared.response.ApiResponse;
import com.novacore.banking.shared.response.SuccessCode;
import com.novacore.banking.application.CustomerService;
import com.novacore.banking.application.dtos.CreateCustomerCommand;
import com.novacore.banking.application.dtos.CustomerResponse;
import com.novacore.banking.application.dtos.UpdateKycStatusCommand;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {
    private final CustomerService customerService;

    @PostMapping    
    public ResponseEntity<ApiResponse<CustomerResponse>> registerCustomer(@Valid @RequestBody CreateCustomerCommand command) {
        CustomerResponse response = customerService.registerCustomer(command);
        return ApiResponse.success(SuccessCode.CUSTOMER_REGISTERED_SUCCESS, response);
    }

    @PatchMapping("/{id}/kyc")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateKycStatus(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateKycStatusCommand command
    ) {
        CustomerResponse response = customerService.updateKycStatus(id, command);
        return ApiResponse.success(SuccessCode.CUSTOMER_KYC_UPDATED, response);
    }
}
