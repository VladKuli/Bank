package com.bank.bank_system.web.client;

import com.bank.bank_system.domain.Account;
import com.bank.bank_system.domain.Client;

import java.util.List;
import java.util.UUID;

public record RecipientResponse(UUID id, String firstName, String lastName, String maskedEmail,
                                List<AccountOption> accounts) {
    public static RecipientResponse from(Client client, List<Account> accounts) {
        String email = client.getEmail() == null ? "" : client.getEmail();
        int at = email.indexOf('@');
        String masked = at <= 1 ? email : email.substring(0, 1) + "•••" + email.substring(at);
        return new RecipientResponse(client.getId(), client.getFirstName(), client.getLastName(), masked,
                accounts.stream().filter(a -> a.getStatus().name().equals("OPEN"))
                        .map(a -> new AccountOption(a.getId(), "•••• " + a.getAccountNumber().substring(Math.max(0, a.getAccountNumber().length() - 4)), a.getCurrency())).toList());
    }

    public record AccountOption(UUID id, String maskedAccount, String currency) {
    }
}
