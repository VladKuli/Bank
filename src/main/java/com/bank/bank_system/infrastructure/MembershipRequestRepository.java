package com.bank.bank_system.infrastructure;

import com.bank.bank_system.domain.MembershipRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MembershipRequestRepository extends JpaRepository<MembershipRequest, UUID> {
    List<MembershipRequest> findByOwnerIdOrderByRequestedAtDesc(UUID ownerId);

    List<MembershipRequest> findByStatusOrderByRequestedAtAsc(String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT request FROM MembershipRequest request WHERE request.id = :id")
    Optional<MembershipRequest> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByOwnerIdAndStatus(UUID ownerId, String status);
}
