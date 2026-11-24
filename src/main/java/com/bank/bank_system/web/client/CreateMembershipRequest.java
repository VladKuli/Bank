package com.bank.bank_system.web.client;

import com.bank.bank_system.domain.enums.MembershipTier;
import jakarta.validation.constraints.NotNull;

public record CreateMembershipRequest(@NotNull MembershipTier tier) {}
