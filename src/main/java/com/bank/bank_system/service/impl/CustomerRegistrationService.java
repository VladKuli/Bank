package com.bank.bank_system.service.impl;

import com.bank.bank_system.domain.Client;
import com.bank.bank_system.domain.enums.AccountType;
import com.bank.bank_system.domain.enums.MembershipTier;
import com.bank.bank_system.infrastructure.ClientRepository;
import com.bank.bank_system.service.AccountService;
import java.util.Locale;
import java.util.UUID;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CustomerRegistrationService {
    private final ClientRepository clientRepository;
    private final AccountService accountService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Client register(String firstName, String lastName, String email, String phone, String password, String confirmation) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (!password.equals(confirmation)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passwords do not match");
        }
        if (clientRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        String personalCode;
        do {
            personalCode = "NB" + UUID.randomUUID().toString().replace("-", "").substring(0, 30).toUpperCase(Locale.ROOT);
        } while (clientRepository.existsByPersonalCode(personalCode));
        Client client = new Client(firstName.trim(), lastName.trim(), personalCode,
                phone == null || phone.isBlank() ? null : phone.trim(), normalizedEmail);
        client.setPasswordHash(passwordEncoder.encode(password));
        client.setMembershipTier(MembershipTier.STANDARD);
        client.setTermsAcceptedAt(Instant.now());
        clientRepository.saveAndFlush(client);
        accountService.open(client.getId(), AccountType.CURRENT, "EUR");
        return client;
    }
}
