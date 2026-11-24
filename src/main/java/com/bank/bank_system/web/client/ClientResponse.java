package com.bank.bank_system.web.client;

import com.bank.bank_system.domain.Client;
import com.bank.bank_system.domain.enums.ClientStatus;
import com.bank.bank_system.domain.enums.MembershipTier;
import java.time.Instant;
import java.util.UUID;

public record ClientResponse(
        UUID id,
        String firstName,
        String lastName,
        String personalCode,
        String phone,
        String email,
        ClientStatus status,
        Instant createdAt,
        Instant verificationApprovedAt,
        String verificationApprovedBy,
        MembershipTier membershipTier) {

    public static ClientResponse from(Client client) {
        return new ClientResponse(
                client.getId(),
                client.getFirstName(),
                client.getLastName(),
                client.getPersonalCode(),
                client.getPhone(),
                client.getEmail(),
                client.getStatus(),
                client.getCreatedAt(),
                client.getVerificationApprovedAt(),
                client.getVerificationApprovedBy(),
                client.getMembershipTier());
    }
}
