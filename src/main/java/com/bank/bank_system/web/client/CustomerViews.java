package com.bank.bank_system.web.client;

import com.bank.bank_system.domain.*;
import com.bank.bank_system.domain.enums.MembershipTier;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class CustomerViews {
    private CustomerViews() {}
    public record Card(UUID id, String tier, String lastFour, LocalDate expiresOn, String status,
                       UUID accountId, String accountLastFour, String currency) {
        public static Card from(PaymentCard card) { return new Card(card.getId(), card.getTier().name(), card.getLastFour(), card.getExpiresOn(), card.getStatus(), card.getAccount().getId(), suffix(card.getAccount().getAccountNumber()), card.getAccount().getCurrency()); }
    }
    public record Request(UUID id, String tier, String status, String decisionReason, Instant requestedAt) {
        public static Request from(CardRequest request) { return new Request(request.getId(), request.getTier().name(), request.getStatus(), request.getDecisionReason(), request.getRequestedAt()); }
    }
    public record Membership(UUID id, String currentTier, String requestedTier, String status, String decisionReason, Instant requestedAt) {
        public static Membership from(MembershipRequest request) { return new Membership(request.getId(), request.getOwner().getMembershipTier().name(), request.getRequestedTier().name(), request.getStatus(), request.getDecisionReason(), request.getRequestedAt()); }
    }
    public record Loan(UUID id, BigDecimal amount, int termMonths, String purpose, String status,
                       String decisionReason, Instant requestedAt) {
        public static Loan from(LoanApplication loan) { return new Loan(loan.getId(), loan.getAmount(), loan.getTermMonths(), loan.getPurpose(), loan.getStatus(), loan.getDecisionReason(), loan.getRequestedAt()); }
    }
    public record Support(UUID id, String category, String subject, String message, String employeeResponse,
                          String status, Instant createdAt, Instant updatedAt, String ownerName, String ownerEmail) {
        public static Support from(SupportRequest request) { return new Support(request.getId(), request.getCategory(), request.getSubject(), request.getMessage(), request.getEmployeeResponse(), request.getStatus(), request.getCreatedAt(), request.getUpdatedAt(), request.getOwner().getFirstName() + " " + request.getOwner().getLastName(), request.getOwner().getEmail()); }
    }
    public record EmployeeReview(UUID id, UUID customerId, String customerName, String customerEmail,
                                 String requestedValue, String status, String detail, Instant requestedAt) {}
    private static String suffix(String value) { return "•••• " + value.substring(Math.max(0, value.length() - 4)); }
}
