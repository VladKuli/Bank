package com.bank.bank_system.web.client;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CustomerProfileUpdateRequest(@Size(max = 32) @Pattern(regexp = "^$|^[+0-9 ()-]{6,32}$") String phone) {}
