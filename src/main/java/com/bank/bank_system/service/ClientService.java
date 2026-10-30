package com.bank.bank_system.service;

import com.bank.bank_system.domain.Client;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.UUID;

@Validated
public interface ClientService {

    Client register(@NotBlank String firstName,
                    @NotBlank String lastName,
                    @NotBlank String personalCode,
                    @NotBlank String phone,
                    @NotBlank String email);

    List<Client> search(String query);

    Client get(@NotNull UUID id);

    Client approveVerification(@NotNull UUID id, @NotBlank String approvedBy);

}


