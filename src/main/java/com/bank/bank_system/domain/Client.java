package com.bank.bank_system.domain;

import com.bank.bank_system.domain.enums.ClientStatus;
import com.bank.bank_system.domain.enums.MembershipTier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "clients")
@AllArgsConstructor
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "personal_code", nullable = false, unique = true, length = 32)
    private String personalCode;

    @Column(length = 32)
    private String phone;

    @Column(length = 254)
    private String email;

    @Column(name = "password_hash", columnDefinition = "text")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "membership_tier", nullable = false, length = 16)
    private MembershipTier membershipTier = MembershipTier.STANDARD;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ClientStatus status = ClientStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "terms_accepted_at")
    private Instant termsAcceptedAt;

    @Column(name = "verification_approved_at")
    private Instant verificationApprovedAt;

    @Column(name = "verification_approved_by", length = 100)
    private String verificationApprovedBy;

    protected Client() {
    }

    public Client(String firstName, String lastName, String personalCode, String phone, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.personalCode = personalCode;
        this.phone = phone;
        this.email = email;
        this.status = ClientStatus.REQUIRES_VERIFICATION;
        this.membershipTier = MembershipTier.STANDARD;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public UUID id() {
        return id;
    }

    public MembershipTier membershipTier() {
        return membershipTier;
    }

    public void setMembershipTier(MembershipTier membershipTier) {
        this.membershipTier = membershipTier;
    }

    public void setTermsAcceptedAt(Instant termsAcceptedAt) {
        this.termsAcceptedAt = termsAcceptedAt;
    }

    public void approveVerification(String approvedBy) {
        if (status != ClientStatus.REQUIRES_VERIFICATION) {
            throw new IllegalStateException("Client is not awaiting verification");
        }
        status = ClientStatus.ACTIVE;
        verificationApprovedAt = Instant.now();
        verificationApprovedBy = approvedBy;
    }

    @PrePersist
    void setCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

}
