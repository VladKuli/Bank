package com.bank.bank_system.web.admin;

import com.bank.bank_system.domain.Employee;
import com.bank.bank_system.service.impl.EmployeeServiceImpl;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.security.Principal;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeServiceImpl employeeServiceImpl;

    @GetMapping
    public List<EmployeeResponse> list() {
        return employeeServiceImpl.list().stream().map(EmployeeResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody CreateEmployeeRequest request) {
        Employee employee = employeeServiceImpl.create(request.username(), request.password(), request.role());
        return ResponseEntity.created(URI.create("/api/admin/employees/" + employee.getId()))
                .body(EmployeeResponse.from(employee));
    }

    @PutMapping("/{employeeId}/enabled")
    public EmployeeResponse setEnabled(
            @PathVariable UUID employeeId,
            @Valid @RequestBody SetEmployeeEnabledRequest request,
            Principal principal) {
        return EmployeeResponse.from(
                employeeServiceImpl.setEnabled(employeeId, request.enabled(), principal.getName()));
    }
}
