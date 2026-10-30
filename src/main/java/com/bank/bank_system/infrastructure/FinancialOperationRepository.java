package com.bank.bank_system.infrastructure;

import com.bank.bank_system.domain.FinancialOperation;
import com.bank.bank_system.domain.enums.FinancialOperationType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FinancialOperationRepository extends JpaRepository<FinancialOperation, UUID> {

    @Query("""
            SELECT o FROM FinancialOperation o
            LEFT JOIN FETCH o.sourceAccount source
            LEFT JOIN FETCH source.owner
            LEFT JOIN FETCH o.targetAccount target
            LEFT JOIN FETCH target.owner
            WHERE o.sourceAccount.id = :accountId OR o.targetAccount.id = :accountId
            ORDER BY o.occurredAt DESC
            """)
    List<FinancialOperation> findHistory(@Param("accountId") UUID accountId);

    @Query("""
            SELECT o FROM FinancialOperation o
            LEFT JOIN FETCH o.sourceAccount source
            LEFT JOIN FETCH source.owner
            LEFT JOIN FETCH o.targetAccount target
            LEFT JOIN FETCH target.owner
            WHERE o.id = :operationId AND
                ((o.sourceAccount IS NOT NULL AND o.sourceAccount.owner.id = :ownerId) OR
                 (o.targetAccount IS NOT NULL AND o.targetAccount.owner.id = :ownerId))
            """)
    java.util.Optional<FinancialOperation> findByIdForOwner(
            @Param("operationId") UUID operationId, @Param("ownerId") UUID ownerId);

    @Query("""
            SELECT o FROM FinancialOperation o
            LEFT JOIN FETCH o.sourceAccount source
            LEFT JOIN FETCH source.owner
            LEFT JOIN FETCH o.targetAccount target
            LEFT JOIN FETCH target.owner
            WHERE o.idempotencyKey = :idempotencyKey
            """)
    java.util.Optional<FinancialOperation> findByIdempotencyKey(@Param("idempotencyKey") UUID idempotencyKey);

    @Query("""
            SELECT o FROM FinancialOperation o
            LEFT JOIN FETCH o.sourceAccount source LEFT JOIN FETCH source.owner
            LEFT JOIN FETCH o.targetAccount target LEFT JOIN FETCH target.owner
            WHERE o.type = :type ORDER BY o.occurredAt DESC
            """)
    List<FinancialOperation> findAllTransfers(@Param("type") FinancialOperationType type);
}
