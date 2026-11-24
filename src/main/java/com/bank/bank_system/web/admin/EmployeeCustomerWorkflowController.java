package com.bank.bank_system.web.admin;

import com.bank.bank_system.service.impl.CustomerBankingService;
import com.bank.bank_system.service.impl.EmployeeDecisionService;
import com.bank.bank_system.web.client.CustomerViews;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employee")
@RequiredArgsConstructor
public class EmployeeCustomerWorkflowController {
    private final CustomerBankingService banking;
    private final EmployeeDecisionService decisions;

    @GetMapping("/card-requests")
    public List<CustomerViews.EmployeeReview> cardRequests() { return banking.pendingCardRequests(); }

    @PatchMapping("/card-requests/{id}")
    public CustomerViews.Request decideCard(@PathVariable UUID id, @Valid @RequestBody EmployeeDecisionRequest request, Principal principal) {
        return decisions.decideCardRequest(id, request.status(), request.reason(), principal.getName());
    }

    @GetMapping("/membership-requests")
    public List<CustomerViews.EmployeeReview> membershipRequests() { return banking.pendingMembershipRequests(); }

    @PatchMapping("/membership-requests/{id}")
    public CustomerViews.Membership decideMembership(@PathVariable UUID id, @Valid @RequestBody EmployeeDecisionRequest request, Principal principal) {
        return decisions.decideMembershipRequest(id, request.status(), request.reason(), principal.getName());
    }

    @GetMapping("/loan-applications")
    public List<CustomerViews.EmployeeReview> loans() { return banking.pendingLoans(); }

    @PatchMapping("/loan-applications/{id}")
    public CustomerViews.Loan decideLoan(@PathVariable UUID id, @Valid @RequestBody EmployeeDecisionRequest request, Principal principal) {
        return decisions.decideLoan(id, request.status(), request.reason(), principal.getName());
    }

    @GetMapping("/support-requests")
    public List<CustomerViews.Support> support() { return banking.allSupportRequests(); }

    @GetMapping("/transfers")
    public List<EmployeeTransferResponse> transfers(@RequestParam(required = false) String q) {
        var rows = banking.allTransfers().stream().map(EmployeeTransferResponse::from).toList();
        if (q == null || q.isBlank()) return rows;
        String query = q.trim().toLowerCase(java.util.Locale.ROOT);
        return rows.stream().filter(r -> (r.reference() + " " + r.sender() + " " + r.recipient()).toLowerCase(java.util.Locale.ROOT).contains(query)).toList();
    }

    @PatchMapping("/support-requests/{id}")
    public CustomerViews.Support updateSupport(@PathVariable UUID id, @Valid @RequestBody EmployeeSupportUpdateRequest request) {
        return banking.updateSupport(id, request.response().trim(), request.status());
    }

}
