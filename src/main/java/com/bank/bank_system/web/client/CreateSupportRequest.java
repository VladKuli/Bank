package com.bank.bank_system.web.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSupportRequest(@NotBlank @Size(max = 40) String category,
                                   @NotBlank @Size(max = 160) String subject,
                                   @NotBlank @Size(max = 5000) String message) {}
