package com.bank.bank_system.infrastructure;

import com.bank.bank_system.domain.Account;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    Boolean existsByAccountNumber(String accountNumber);

    @Query("SELECT a FROM Account a JOIN FETCH a.owner WHERE a.owner.id = :ownerId ORDER BY a.openedAt DESC")
    List<Account> findByOwnerIdOrderByOpenedAtDesc(@Param("ownerId") UUID ownerId);

    boolean existsByIdAndOwnerId(UUID id, UUID ownerId);

    @Query("SELECT a FROM Account a JOIN FETCH a.owner WHERE a.id = :id")
    Optional<Account> findByIdWithOwner(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a JOIN FETCH a.owner WHERE a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") UUID id);
}
