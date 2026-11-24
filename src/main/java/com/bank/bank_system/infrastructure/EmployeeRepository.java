package com.bank.bank_system.infrastructure;

import com.bank.bank_system.domain.Employee;
import com.bank.bank_system.domain.enums.EmployeeRole;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    Optional<Employee> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    List<Employee> findAllByOrderByUsernameAsc();

    long countByRoleAndEnabledTrue(EmployeeRole role);
}
