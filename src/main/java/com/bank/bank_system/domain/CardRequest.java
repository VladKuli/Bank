package com.bank.bank_system.domain;

import com.bank.bank_system.domain.enums.MembershipTier;
import jakarta.persistence.*;
import lombok.Getter;
import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "card_requests")
public class CardRequest {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "owner_id") private Client owner;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "account_id") private Account account;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private MembershipTier tier;
    @Column(nullable = false, length = 16) private String status = "PENDING";
    @Column(name = "decision_reason") private String decisionReason;
    @Column(name = "requested_at", nullable = false, updatable = false) private Instant requestedAt;
    @Column(name = "decided_at") private Instant decidedAt;
    @Column(name = "decided_by") private String decidedBy;
    protected CardRequest() {}
    public CardRequest(Client owner, Account account, MembershipTier tier) { this.owner = owner; this.account = account; this.tier = tier; }
    public void decide(String status, String reason, String employee) { this.status = status; this.decisionReason = reason; this.decidedBy = employee; this.decidedAt = Instant.now(); }
    public Client ownerForDecision() { return owner; }
    public Account accountForDecision() { return account; }
    public MembershipTier tierForDecision() { return tier; }
    public String status() { return status; }
    @PrePersist void setRequestedAt() { if (requestedAt == null) requestedAt = Instant.now(); }
}
