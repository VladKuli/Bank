package com.bank.bank_system.infrastructure;

import com.bank.bank_system.domain.CardRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRequestRepository extends JpaRepository<CardRequest, UUID> {
    List<CardRequest> findByOwnerIdOrderByRequestedAtDesc(UUID ownerId);
    List<CardRequest> findByStatusOrderByRequestedAtAsc(String status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT request FROM CardRequest request WHERE request.id = :id")
    Optional<CardRequest> findByIdForUpdate(@Param("id") UUID id);
    boolean existsByOwnerIdAndAccountIdAndTierAndStatus(UUID ownerId, UUID accountId, com.bank.bank_system.domain.enums.MembershipTier tier, String status);
}
