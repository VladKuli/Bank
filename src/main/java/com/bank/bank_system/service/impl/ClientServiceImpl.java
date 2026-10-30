package com.bank.bank_system.service.impl;

import com.bank.bank_system.exception.ClientNotFoundException;
import com.bank.bank_system.exception.DuplicatePersonalCodeException;
import com.bank.bank_system.domain.Client;
import com.bank.bank_system.domain.enums.ClientStatus;
import com.bank.bank_system.infrastructure.ClientRepository;
import java.util.List;
import java.util.UUID;

import com.bank.bank_system.service.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;

    @Override
    @Transactional
    public Client register(String firstName, String lastName, String personalCode, String phone, String email) {
        if (clientRepository.existsByPersonalCode(personalCode)) {
            throw new DuplicatePersonalCodeException(personalCode);
        }

        return clientRepository.save(new Client(firstName, lastName, personalCode, phone, email));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Client> search(String query) {
        if (query == null || query.isBlank()) {
            return clientRepository.findAllByOrderByLastNameAscFirstNameAsc();
        }
        return clientRepository.search(query.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public Client get(UUID id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException(id));
    }

    @Override
    @Transactional
    public Client approveVerification(UUID id, String approvedBy) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException(id));
        if (client.getStatus() != ClientStatus.REQUIRES_VERIFICATION) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Client is not awaiting verification");
        }
        client.approveVerification(approvedBy);
        return client;
    }

}
