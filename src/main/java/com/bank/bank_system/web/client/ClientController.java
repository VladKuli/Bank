package com.bank.bank_system.web.client;

import com.bank.bank_system.exception.ClientNotFoundException;
import com.bank.bank_system.service.ClientService;
import com.bank.bank_system.exception.DuplicatePersonalCodeException;
import com.bank.bank_system.domain.Client;
import com.bank.bank_system.web.ErrorResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.security.Principal;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientServiceImpl;

    @PostMapping
    public ResponseEntity<ClientResponse> register(@Valid @RequestBody CreateClientRequest request) {
        Client client = clientServiceImpl.register(
                request.firstName().trim(),
                request.lastName().trim(),
                request.personalCode().trim(),
                trimToNull(request.phone()),
                trimToNull(request.email()));

        return ResponseEntity.created(URI.create("/api/clients/" + client.getId()))
                .body(ClientResponse.from(client));
    }

    @GetMapping
    public List<ClientResponse> search(@RequestParam(required = false) String q) {
        return clientServiceImpl.search(q).stream().map(ClientResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ClientResponse get(@PathVariable UUID id) {
        return ClientResponse.from(clientServiceImpl.get(id));
    }

    @PatchMapping("/{id}/verification")
    public ClientResponse approveVerification(@PathVariable UUID id, Principal principal) {
        return ClientResponse.from(clientServiceImpl.approveVerification(id, principal.getName()));
    }

    @ExceptionHandler(DuplicatePersonalCodeException.class)
    public ResponseEntity<ErrorResponse> duplicatePersonalCode(DuplicatePersonalCodeException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(ClientNotFoundException.class)
    public ResponseEntity<ErrorResponse> clientNotFound(ClientNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(exception.getMessage()));
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

}
