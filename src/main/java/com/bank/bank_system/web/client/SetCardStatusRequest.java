package com.bank.bank_system.web.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SetCardStatusRequest(@NotBlank @Pattern(regexp = "ACTIVE|FROZEN|BLOCKED") String status) {}
