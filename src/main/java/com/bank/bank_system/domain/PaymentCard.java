package com.bank.bank_system.domain;

import com.bank.bank_system.domain.enums.MembershipTier;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Entity
@Table(name = "payment_cards")
public class PaymentCard {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private Client owner;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id")
    private Account account;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MembershipTier tier;
    @Column(name = "last_four", nullable = false, length = 4)
    private String lastFour;
    @Column(name = "expires_on", nullable = false)
    private LocalDate expiresOn;
    @Column(nullable = false, length = 16)
    private String status = "ACTIVE";
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PaymentCard() {
    }

    public PaymentCard(Client owner, Account account, MembershipTier tier, String lastFour) {
        this.owner = owner;
        this.account = account;
        this.tier = tier;
        this.lastFour = lastFour;
        this.expiresOn = LocalDate.now().plusYears(4).withDayOfMonth(1).minusDays(1);
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String status() {
        return status;
    }

    @PrePersist
    void setCreatedAt() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
