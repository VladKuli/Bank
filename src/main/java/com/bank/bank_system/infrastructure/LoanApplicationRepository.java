package com.bank.bank_system.infrastructure;

import com.bank.bank_system.domain.LoanApplication;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, UUID> {
    List<LoanApplication> findByOwnerIdOrderByRequestedAtDesc(UUID ownerId);

    List<LoanApplication> findByStatusOrderByRequestedAtAsc(String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT application FROM LoanApplication application WHERE application.id = :id")
    Optional<LoanApplication> findByIdForUpdate(@Param("id") UUID id);
}
