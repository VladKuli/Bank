package com.bank.bank_system.infrastructure;

import com.bank.bank_system.domain.SupportRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportRequestRepository extends JpaRepository<SupportRequest, UUID> {
    List<SupportRequest> findByOwnerIdOrderByUpdatedAtDesc(UUID ownerId);

    List<SupportRequest> findAllByOrderByUpdatedAtDesc();

    Optional<SupportRequest> findByIdAndOwnerId(UUID id, UUID ownerId);
}
