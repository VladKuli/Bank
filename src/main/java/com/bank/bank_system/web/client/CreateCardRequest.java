package com.bank.bank_system.web.client;

import com.bank.bank_system.domain.enums.MembershipTier;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateCardRequest(@NotNull UUID accountId, @NotNull MembershipTier tier) {}
