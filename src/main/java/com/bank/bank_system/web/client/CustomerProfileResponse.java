package com.bank.bank_system.web.client;

import com.bank.bank_system.domain.Client;
import com.bank.bank_system.domain.enums.MembershipTier;
import java.time.Instant;
import java.util.UUID;

public record CustomerProfileResponse(UUID id, String firstName, String lastName, String email, String phone,
                                      String customerNumber, MembershipTier membershipTier, Instant joinedAt) {
    public static CustomerProfileResponse from(Client client) {
        return new CustomerProfileResponse(client.getId(), client.getFirstName(), client.getLastName(),
                client.getEmail(), client.getPhone(), client.getPersonalCode(), client.getMembershipTier(), client.getCreatedAt());
    }
}
