package com.bank.bank_system.web.admin;

import jakarta.validation.constraints.NotNull;

public record SetEmployeeEnabledRequest(@NotNull Boolean enabled) {
}
