package com.bank.bank_system.domain;

import com.bank.bank_system.domain.enums.MembershipTier;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "membership_requests")
public class MembershipRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private Client owner;
    @Enumerated(EnumType.STRING)
    @Column(name = "requested_tier", nullable = false, length = 16)
    private MembershipTier requestedTier;
    @Column(nullable = false, length = 16)
    private String status = "PENDING";
    @Column(name = "decision_reason")
    private String decisionReason;
    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;
    @Column(name = "decided_at")
    private Instant decidedAt;
    @Column(name = "decided_by")
    private String decidedBy;

    protected MembershipRequest() {
    }

    public MembershipRequest(Client owner, MembershipTier requestedTier) {
        this.owner = owner;
        this.requestedTier = requestedTier;
    }

    public void decide(String status, String reason, String employee) {
        this.status = status;
        this.decisionReason = reason;
        this.decidedBy = employee;
        this.decidedAt = Instant.now();
    }

    public Client ownerForDecision() {
        return owner;
    }

    public MembershipTier requestedTierForDecision() {
        return requestedTier;
    }

    public String status() {
        return status;
    }

    @PrePersist
    void setRequestedAt() {
        if (requestedAt == null) requestedAt = Instant.now();
    }
}
