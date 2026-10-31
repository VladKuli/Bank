package com.bank.bank_system.infrastructure;

import com.bank.bank_system.domain.PaymentCard;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentCardRepository extends JpaRepository<PaymentCard, UUID> {
    List<PaymentCard> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT card FROM PaymentCard card WHERE card.id = :id")
    Optional<PaymentCard> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByIdAndOwnerId(UUID id, UUID ownerId);

    boolean existsByOwnerIdAndAccountIdAndTierAndStatusNot(UUID ownerId, UUID accountId, com.bank.bank_system.domain.enums.MembershipTier tier, String status);
}
