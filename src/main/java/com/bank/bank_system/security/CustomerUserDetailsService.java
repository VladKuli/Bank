package com.bank.bank_system.security;

import com.bank.bank_system.domain.Client;
import com.bank.bank_system.infrastructure.ClientRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerUserDetailsService implements UserDetailsService {
    private final ClientRepository clientRepository;

    public CustomerUserDetailsService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Client client = clientRepository.findByEmailIgnoreCaseAndPasswordHashIsNotNull(email.trim())
                .orElseThrow(() -> new UsernameNotFoundException("Customer account was not found"));
        return User.withUsername(client.getEmail())
                .password(client.getPasswordHash())
                .roles("CUSTOMER")
                .disabled(client.getStatus() == com.bank.bank_system.domain.enums.ClientStatus.BLOCKED)
                .build();
    }
}
