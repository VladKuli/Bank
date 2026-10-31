package com.bank.bank_system.web.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmployeeSupportUpdateRequest(@NotBlank @Size(max = 5000) String response,
        @NotBlank @Pattern(regexp = "IN_PROGRESS|WAITING_FOR_CUSTOMER|RESOLVED") String status) {}
