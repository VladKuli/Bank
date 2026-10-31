package com.bank.bank_system.service.impl;

import com.bank.bank_system.domain.*;
import com.bank.bank_system.domain.enums.MembershipTier;
import com.bank.bank_system.domain.enums.FinancialOperationType;
import com.bank.bank_system.infrastructure.*;
import com.bank.bank_system.service.AccountService;
import com.bank.bank_system.web.client.CustomerViews;
import com.bank.bank_system.web.client.CustomerProfileResponse;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CustomerBankingService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final ClientRepository clients;
    private final AccountService accounts;
    private final FinancialOperationRepository operations;
    private final PaymentCardRepository cards;
    private final CardRequestRepository cardRequests;
    private final MembershipRequestRepository membershipRequests;
    private final LoanApplicationRepository loans;
    private final SupportRequestRepository supportRequests;

    @Transactional(readOnly = true)
    public Client customer(String email) { return clients.findByEmailIgnoreCaseAndPasswordHashIsNotNull(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer account not found")); }

    @Transactional
    public CustomerProfileResponse updatePhone(String email, String phone) {
        Client client = customer(email);
        client.setPhone(phone == null || phone.isBlank() ? null : phone.trim());
        return CustomerProfileResponse.from(client);
    }

    @Transactional(readOnly = true)
    public List<FinancialOperation> transactions(String email) {
        Client customer = customer(email);
        Map<UUID, FinancialOperation> result = new LinkedHashMap<>();
        accounts.listForClient(customer.getId()).forEach(account -> operations.findHistory(account.getId()).forEach(operation -> result.put(operation.getId(), operation)));
        return result.values().stream().sorted(Comparator.comparing(FinancialOperation::getOccurredAt).reversed()).toList();
    }

    @Transactional(readOnly = true)
    public List<FinancialOperation> accountTransactions(String email, UUID accountId) {
        Client customer = customer(email);
        Account account = accounts.get(accountId);
        if (!account.getOwner().getId().equals(customer.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found");
        }
        return operations.findHistory(accountId);
    }

    @Transactional(readOnly = true)
    public FinancialOperation transaction(String email, UUID id) {
        return operations.findByIdForOwner(id, customer(email).getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
    }

    @Transactional
    public FinancialOperation transfer(String email, UUID sourceId, UUID targetId, BigDecimal amount, String description, UUID idempotencyKey) {
        Client sender = customer(email);
        FinancialOperation existing = operations.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            boolean belongsToSender = existing.getSourceAccount() != null
                    && existing.getSourceAccount().getOwner().getId().equals(sender.getId());
            if (!belongsToSender) throw new ResponseStatusException(HttpStatus.CONFLICT, "Transfer key has already been used");
            return existing;
        }
        Account source = accounts.get(sourceId);
        Account target = accounts.get(targetId);
        if (!source.getOwner().getId().equals(sender.getId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Source account does not belong to you");
        if (target.getOwner().getId().equals(sender.getId())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose an account belonging to another NordBank customer");
        if (target.getOwner().getPasswordHash() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recipient is not registered for online banking");
        return accounts.transfer(sourceId, targetId, amount, description == null ? null : description.trim(), idempotencyKey);
    }

    @Transactional
    public Object requestCard(String email, UUID accountId, MembershipTier tier) {
        Client owner = customer(email);
        Account account = accounts.get(accountId);
        if (!account.getOwner().getId().equals(owner.getId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account does not belong to you");
        if (!account.getStatus().name().equals("OPEN")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cards can only be linked to open accounts");
        if (rank(tier) > rank(owner.getMembershipTier())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Your membership does not include this card tier");
        if (cards.existsByOwnerIdAndAccountIdAndTierAndStatusNot(owner.getId(), accountId, tier, "BLOCKED")
                || cardRequests.existsByOwnerIdAndAccountIdAndTierAndStatus(owner.getId(), accountId, tier, "PENDING")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A card of this tier is already active or pending for this account");
        }
        if (tier == MembershipTier.STANDARD) {
            return CustomerViews.Card.from(cards.save(new PaymentCard(owner, account, tier, lastFour())));
        }
        return CustomerViews.Request.from(cardRequests.save(new CardRequest(owner, account, tier)));
    }

    @Transactional(readOnly = true)
    public List<CustomerViews.Card> cards(String email) { return cards.findByOwnerIdOrderByCreatedAtDesc(customer(email).getId()).stream().map(CustomerViews.Card::from).toList(); }

    @Transactional(readOnly = true)
    public List<CustomerViews.Request> cardRequests(String email) { return cardRequests.findByOwnerIdOrderByRequestedAtDesc(customer(email).getId()).stream().map(CustomerViews.Request::from).toList(); }

    @Transactional
    public CustomerViews.Membership requestMembership(String email, MembershipTier tier) {
        Client owner = customer(email);
        if (tier == MembershipTier.STANDARD || rank(tier) <= rank(owner.getMembershipTier())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a higher membership tier");
        if (membershipRequests.existsByOwnerIdAndStatus(owner.getId(), "PENDING")) throw new ResponseStatusException(HttpStatus.CONFLICT, "A membership request is already pending");
        return CustomerViews.Membership.from(membershipRequests.save(new MembershipRequest(owner, tier)));
    }

    @Transactional(readOnly = true)
    public List<CustomerViews.Membership> membershipRequests(String email) { return membershipRequests.findByOwnerIdOrderByRequestedAtDesc(customer(email).getId()).stream().map(CustomerViews.Membership::from).toList(); }

    @Transactional
    public CustomerViews.Loan createLoan(String email, BigDecimal amount, int termMonths, String purpose) {
        if (amount == null || amount.compareTo(new BigDecimal("500")) < 0 || amount.compareTo(new BigDecimal("100000")) > 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loan amount must be between €500 and €100,000");
        if (termMonths < 6 || termMonths > 84) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a term between 6 and 84 months");
        return CustomerViews.Loan.from(loans.save(new LoanApplication(customer(email), amount, termMonths, purpose.trim())));
    }

    @Transactional(readOnly = true)
    public List<CustomerViews.Loan> loans(String email) { return loans.findByOwnerIdOrderByRequestedAtDesc(customer(email).getId()).stream().map(CustomerViews.Loan::from).toList(); }

    @Transactional
    public CustomerViews.Support createSupport(String email, String category, String subject, String message) {
        return CustomerViews.Support.from(supportRequests.save(new SupportRequest(customer(email), category.trim(), subject.trim(), message.trim())));
    }

    @Transactional(readOnly = true)
    public List<CustomerViews.Support> supportRequests(String email) { return supportRequests.findByOwnerIdOrderByUpdatedAtDesc(customer(email).getId()).stream().map(CustomerViews.Support::from).toList(); }

    @Transactional(readOnly = true)
    public CustomerViews.Support supportRequest(String email, UUID id) {
        return CustomerViews.Support.from(supportRequests.findByIdAndOwnerId(id, customer(email).getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Support request not found")));
    }

    @Transactional(readOnly = true)
    public List<CustomerViews.EmployeeReview> pendingCardRequests() {
        return cardRequests.findByStatusOrderByRequestedAtAsc("PENDING").stream().map(r -> review(r.getId(), r.getOwner(), r.getTier().name(), r.getStatus(), r.getAccount().getCurrency() + " account", r.getRequestedAt())).toList();
    }

    @Transactional(readOnly = true)
    public List<FinancialOperation> allTransfers() { return operations.findAllTransfers(FinancialOperationType.TRANSFER); }

    @Transactional(readOnly = true)
    public List<CustomerViews.EmployeeReview> pendingMembershipRequests() {
        return membershipRequests.findByStatusOrderByRequestedAtAsc("PENDING").stream().map(r -> review(r.getId(), r.getOwner(), r.getRequestedTier().name(), r.getStatus(), "Upgrade from " + r.getOwner().getMembershipTier(), r.getRequestedAt())).toList();
    }

    @Transactional(readOnly = true)
    public List<CustomerViews.EmployeeReview> pendingLoans() {
        return loans.findByStatusOrderByRequestedAtAsc("PENDING").stream().map(r -> review(r.getId(), r.getOwner(), "€" + r.getAmount() + " · " + r.getTermMonths() + " months", r.getStatus(), r.getPurpose(), r.getRequestedAt())).toList();
    }

    @Transactional(readOnly = true)
    public List<CustomerViews.Support> allSupportRequests() { return supportRequests.findAllByOrderByUpdatedAtDesc().stream().map(CustomerViews.Support::from).toList(); }

    @Transactional
    public CustomerViews.Support updateSupport(UUID id, String response, String status) {
        SupportRequest request = supportRequests.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Support request not found"));
        request.updateByEmployee(response, status);
        return CustomerViews.Support.from(request);
    }

    private CustomerViews.EmployeeReview review(UUID id, Client owner, String value, String status, String detail, Instant at) {
        return new CustomerViews.EmployeeReview(id, owner.getId(), owner.getFirstName() + " " + owner.getLastName(), owner.getEmail(), value, status, detail, at);
    }

    private int rank(MembershipTier tier) { return switch (tier) { case STANDARD -> 0; case PREMIUM -> 1; case VIP -> 2; }; }
    private String lastFour() { return String.format(Locale.ROOT, "%04d", RANDOM.nextInt(10_000)); }
}
