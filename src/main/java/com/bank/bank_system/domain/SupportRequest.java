package com.bank.bank_system.domain;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "support_requests")
public class SupportRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private Client owner;
    @Column(nullable = false, length = 40)
    private String category;
    @Column(nullable = false, length = 160)
    private String subject;
    @Column(nullable = false, columnDefinition = "text")
    private String message;
    @Column(name = "employee_response", columnDefinition = "text")
    private String employeeResponse;
    @Column(nullable = false, length = 24)
    private String status = "OPEN";
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SupportRequest() {
    }

    public SupportRequest(Client owner, String category, String subject, String message) {
        this.owner = owner;
        this.category = category;
        this.subject = subject;
        this.message = message;
    }

    public void updateByEmployee(String response, String status) {
        this.employeeResponse = response;
        this.status = status;
        this.updatedAt = Instant.now();
    }

    @PrePersist
    void setDates() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = createdAt;
    }
}
