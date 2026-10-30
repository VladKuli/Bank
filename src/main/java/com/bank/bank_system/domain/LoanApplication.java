package com.bank.bank_system.domain;

import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "loan_applications")
public class LoanApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private Client owner;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Column(name = "term_months", nullable = false)
    private int termMonths;
    @Column(nullable = false, length = 300)
    private String purpose;
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

    protected LoanApplication() {
    }

    public LoanApplication(Client owner, BigDecimal amount, int termMonths, String purpose) {
        this.owner = owner;
        this.amount = amount;
        this.termMonths = termMonths;
        this.purpose = purpose;
    }

    public void decide(String status, String reason, String employee) {
        this.status = status;
        this.decisionReason = reason;
        this.decidedBy = employee;
        this.decidedAt = Instant.now();
    }

    public String status() {
        return status;
    }

    @PrePersist
    void setRequestedAt() {
        if (requestedAt == null) requestedAt = Instant.now();
    }
}
