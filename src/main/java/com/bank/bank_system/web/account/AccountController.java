package com.bank.bank_system.web.account;

import com.bank.bank_system.domain.Account;
import com.bank.bank_system.service.AccountService;
import com.bank.bank_system.web.operartions.FinancialOperationResponse;
import com.bank.bank_system.web.operartions.MoneyOperationRequest;
import com.bank.bank_system.web.operartions.TransferRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/api/clients/{clientId}/accounts")
    public ResponseEntity<AccountResponse> open(
            @PathVariable UUID clientId,
            @Valid @RequestBody OpenAccountRequest request) {
        Account account = accountService.open(clientId, request.type(), request.currency());
        return ResponseEntity.created(URI.create("/api/accounts/" + account.getId()))
                .body(AccountResponse.from(account));
    }

    @GetMapping("/api/clients/{clientId}/accounts")
    public List<AccountResponse> listForClient(@PathVariable UUID clientId) {
        return accountService.listForClient(clientId).stream().map(AccountResponse::from).toList();
    }

    @GetMapping("/api/accounts/{accountId}")
    public AccountResponse get(@PathVariable UUID accountId) {
        return AccountResponse.from(accountService.get(accountId));
    }

    @PostMapping("/api/accounts/{accountId}/deposits")
    public FinancialOperationResponse deposit(
            @PathVariable UUID accountId,
            @Valid @RequestBody MoneyOperationRequest request) {
        return FinancialOperationResponse.from(accountService.deposit(accountId, request.amount()));
    }

    @PostMapping("/api/accounts/{accountId}/withdrawals")
    public FinancialOperationResponse withdraw(
            @PathVariable UUID accountId,
            @Valid @RequestBody MoneyOperationRequest request) {
        return FinancialOperationResponse.from(accountService.withdraw(accountId, request.amount()));
    }

    @PostMapping("/api/accounts/{accountId}/transfers")
    public FinancialOperationResponse transfer(
            @PathVariable UUID accountId,
            @Valid @RequestBody TransferRequest request) {
        return FinancialOperationResponse.from(
                accountService.transfer(accountId, request.targetAccountId(), request.amount()));
    }

    @PostMapping("/api/accounts/{accountId}/close")
    public AccountResponse close(@PathVariable UUID accountId) {
        return AccountResponse.from(accountService.close(accountId));
    }

    @GetMapping("/api/accounts/{accountId}/operations")
    public List<FinancialOperationResponse> history(@PathVariable UUID accountId) {
        return accountService.history(accountId).stream().map(FinancialOperationResponse::from).toList();
    }
}
