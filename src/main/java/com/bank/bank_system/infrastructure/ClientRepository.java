package com.bank.bank_system.infrastructure;

import com.bank.bank_system.domain.Client;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClientRepository extends JpaRepository<Client, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT client FROM Client client WHERE client.id = :id")
    Optional<Client> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByPersonalCode(String personalCode);

    boolean existsByEmailIgnoreCase(String email);

    java.util.Optional<Client> findByEmailIgnoreCaseAndPasswordHashIsNotNull(String email);

    @Query("""
            SELECT c FROM Client c
            WHERE c.passwordHash IS NOT NULL AND c.id <> :currentId
              AND (LOWER(c.firstName) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(COALESCE(c.email, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(c.personalCode) LIKE LOWER(CONCAT('%', :query, '%')))
            ORDER BY c.lastName, c.firstName
            """)
    List<Client> searchRegistered(@Param("query") String query, @Param("currentId") UUID currentId);

    List<Client> findAllByOrderByLastNameAscFirstNameAsc();

    @Query("""
            SELECT c FROM Client c
            WHERE LOWER(c.firstName) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(c.personalCode) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(COALESCE(c.phone, '')) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(COALESCE(c.email, '')) LIKE LOWER(CONCAT('%', :query, '%'))
            ORDER BY c.lastName, c.firstName
            """)
    List<Client> search(@Param("query") String query);
}
