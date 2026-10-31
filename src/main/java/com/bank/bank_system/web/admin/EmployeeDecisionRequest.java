package com.bank.bank_system.web.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmployeeDecisionRequest(@NotBlank @Pattern(regexp = "APPROVED|REJECTED") String status,
                                      @Size(max = 500) String reason) {}
