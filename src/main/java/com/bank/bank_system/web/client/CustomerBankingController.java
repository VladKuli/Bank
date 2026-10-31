package com.bank.bank_system.web.client;

import com.bank.bank_system.domain.Client;
import com.bank.bank_system.domain.enums.AccountStatus;
import com.bank.bank_system.service.AccountService;
import com.bank.bank_system.service.impl.CustomerAccountStatusService;
import com.bank.bank_system.service.impl.CustomerCardStatusService;
import com.bank.bank_system.service.impl.CustomerBankingService;
import com.bank.bank_system.web.account.AccountResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerBankingController {
    private final CustomerBankingService banking;
    private final CustomerAccountStatusService accountStatusService;
    private final CustomerCardStatusService cardStatusService;
    private final AccountService accounts;
    private final com.bank.bank_system.infrastructure.ClientRepository clients;

    @GetMapping("/me")
    public CustomerProfileResponse me(Principal principal) { return CustomerProfileResponse.from(banking.customer(principal.getName())); }

    @PatchMapping("/me")
    public CustomerProfileResponse updateProfile(Principal principal, @Valid @RequestBody CustomerProfileUpdateRequest request) {
        return banking.updatePhone(principal.getName(), request.phone());
    }

    @GetMapping("/accounts")
    public List<AccountResponse> accounts(Principal principal) {
        return accounts.listForClient(banking.customer(principal.getName()).getId()).stream().map(AccountResponse::from).toList();
    }

    @PatchMapping("/accounts/{accountId}/status")
    public AccountResponse updateAccountStatus(
            Principal principal,
            @PathVariable UUID accountId,
            @Valid @RequestBody SetCustomerAccountStatusRequest request) {
        Client customer = banking.customer(principal.getName());
        return AccountResponse.from(accountStatusService.updateStatus(
                customer.getId(), accountId, AccountStatus.valueOf(request.status())));
    }

    @GetMapping("/accounts/{accountId}/transactions")
    public List<CustomerTransactionResponse> accountTransactions(Principal principal, @PathVariable UUID accountId) {
        Client customer = banking.customer(principal.getName());
        return banking.accountTransactions(principal.getName(), accountId).stream()
                .map(operation -> CustomerTransactionResponse.from(operation, customer.getId())).toList();
    }

    @GetMapping("/transactions")
    public List<CustomerTransactionResponse> transactions(Principal principal) {
        Client customer = banking.customer(principal.getName());
        return banking.transactions(principal.getName()).stream().map(operation -> CustomerTransactionResponse.from(operation, customer.getId())).toList();
    }

    @GetMapping("/transactions/{id}")
    public CustomerTransactionResponse transaction(Principal principal, @PathVariable UUID id) {
        Client customer = banking.customer(principal.getName());
        return CustomerTransactionResponse.from(banking.transaction(principal.getName(), id), customer.getId());
    }

    @GetMapping("/recipients")
    public List<RecipientResponse> recipients(Principal principal, @RequestParam String q) {
        Client customer = banking.customer(principal.getName());
        if (q == null || q.trim().length() < 2) return List.of();
        return clients.searchRegistered(q.trim(), customer.getId()).stream()
                .map(recipient -> RecipientResponse.from(recipient, accounts.listForClient(recipient.getId())))
                .filter(recipient -> !recipient.accounts().isEmpty()).limit(10).toList();
    }

    @PostMapping("/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerTransactionResponse transfer(Principal principal, @Valid @RequestBody CreateTransferRequest request) {
        Client customer = banking.customer(principal.getName());
        var operation = banking.transfer(principal.getName(), request.sourceAccountId(), request.targetAccountId(), request.amount(), request.description(), request.idempotencyKey());
        return CustomerTransactionResponse.from(operation, customer.getId());
    }

    @GetMapping("/cards")
    public List<CustomerViews.Card> cards(Principal principal) { return banking.cards(principal.getName()); }

    @PostMapping("/cards")
    @ResponseStatus(HttpStatus.CREATED)
    public Object requestCard(Principal principal, @Valid @RequestBody CreateCardRequest request) {
        return banking.requestCard(principal.getName(), request.accountId(), request.tier());
    }

    @PatchMapping("/cards/{id}")
    public CustomerViews.Card cardStatus(Principal principal, @PathVariable UUID id, @Valid @RequestBody SetCardStatusRequest request) {
        Client customer = banking.customer(principal.getName());
        return cardStatusService.updateStatus(customer.getId(), id, request.status());
    }

    @GetMapping("/card-requests")
    public List<CustomerViews.Request> cardRequests(Principal principal) { return banking.cardRequests(principal.getName()); }

    @GetMapping("/membership-requests")
    public List<CustomerViews.Membership> membershipRequests(Principal principal) { return banking.membershipRequests(principal.getName()); }

    @PostMapping("/membership-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerViews.Membership membershipRequest(Principal principal, @Valid @RequestBody CreateMembershipRequest request) {
        return banking.requestMembership(principal.getName(), request.tier());
    }

    @GetMapping("/loans")
    public List<CustomerViews.Loan> loans(Principal principal) { return banking.loans(principal.getName()); }

    @PostMapping("/loans")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerViews.Loan createLoan(Principal principal, @Valid @RequestBody CreateLoanRequest request) {
        return banking.createLoan(principal.getName(), request.amount(), request.termMonths(), request.purpose());
    }

    @GetMapping("/support")
    public List<CustomerViews.Support> support(Principal principal) { return banking.supportRequests(principal.getName()); }

    @GetMapping("/support/{id}")
    public CustomerViews.Support supportDetail(Principal principal, @PathVariable UUID id) { return banking.supportRequest(principal.getName(), id); }

    @PostMapping("/support")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerViews.Support createSupport(Principal principal, @Valid @RequestBody CreateSupportRequest request) {
        return banking.createSupport(principal.getName(), request.category(), request.subject(), request.message());
    }
}
