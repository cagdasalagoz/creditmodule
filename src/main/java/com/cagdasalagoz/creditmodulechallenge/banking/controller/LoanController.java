package com.cagdasalagoz.creditmodulechallenge.banking.controller;

import com.cagdasalagoz.creditmodulechallenge.banking.dto.Customer;
import com.cagdasalagoz.creditmodulechallenge.banking.model.request.CreateLoanRequest;
import com.cagdasalagoz.creditmodulechallenge.banking.model.request.PayLoanRequest;
import com.cagdasalagoz.creditmodulechallenge.banking.model.response.CreateLoanResponse;
import com.cagdasalagoz.creditmodulechallenge.banking.model.response.InstallmentResponse;
import com.cagdasalagoz.creditmodulechallenge.banking.model.response.PayLoanResponse;
import com.cagdasalagoz.creditmodulechallenge.banking.repository.CustomerRepository;
import com.cagdasalagoz.creditmodulechallenge.banking.service.LoanService;
import com.cagdasalagoz.creditmodulechallenge.banking.enums.Role;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
@Slf4j
public class LoanController {

    private final LoanService loanService;
    private final CustomerRepository customerRepository;

    private Long getAuthenticatedCustomerId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("User is not authenticated.");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            Optional<Customer> customerOpt = customerRepository.findByUser_Username(username);
            return customerOpt.map(Customer::getId)
                    .orElseThrow(() -> new AccessDeniedException("Authenticated user not found in customer database."));
        }
        throw new AccessDeniedException("Invalid authentication principal.");
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CreateLoanResponse> createLoan(@Valid @RequestBody CreateLoanRequest request) {
        log.info("Received request to create loan: {}", request);
        return ResponseEntity.status(HttpStatus.CREATED).body(loanService.createLoan(request));
    }

    @GetMapping
    public ResponseEntity<List<CreateLoanResponse>> listLoans(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Integer numberOfInstallments,
            @RequestParam(required = false) Boolean isPaid,
            Authentication authentication) {
        log.info("Received request to list loans. CustomerId: {}, Installments: {}, IsPaid: {}",
                 customerId, numberOfInstallments, isPaid);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_" + Role.ADMIN.name()));

        if (isAdmin) {
            return ResponseEntity.ok(loanService.listLoans(customerId, numberOfInstallments, isPaid));
        } else {
            // Customer can only list their own loans
            Long authenticatedCustomerId = getAuthenticatedCustomerId(authentication);
            if (customerId != null && !customerId.equals(authenticatedCustomerId)) {
                throw new AccessDeniedException("Customers can only view their own loans.");
            }
            // If customerId is not provided by a CUSTOMER, use their authenticated ID.
            return ResponseEntity.ok(loanService.listLoansForCustomer(authenticatedCustomerId,
                    numberOfInstallments, isPaid));
        }
    }

    @GetMapping("/{loanId}")
    public ResponseEntity<CreateLoanResponse> getLoanDetails(@PathVariable Long loanId, Authentication authentication) {
        log.info("Received request to get details for loan ID: {}", loanId);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_" + Role.ADMIN.name()));

        if (isAdmin) {
            return ResponseEntity.ok(loanService.getLoanDetails(loanId));
        } else {
            Long authenticatedCustomerId = getAuthenticatedCustomerId(authentication);
            return ResponseEntity.ok(loanService.getLoanDetailsForCustomer(loanId, authenticatedCustomerId));
        }
    }

    @GetMapping("/{loanId}/installments")
    public ResponseEntity<List<InstallmentResponse>> listInstallmentsForLoan(@PathVariable Long loanId,
                                                                             Authentication authentication) {
        log.info("Received request to list installments for loan ID: {}", loanId);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_" + Role.ADMIN.name()));
        if (isAdmin) {
            return ResponseEntity.ok(loanService.listInstallmentsForLoan(loanId));
        } else {
            Long authenticatedCustomerId = getAuthenticatedCustomerId(authentication);
            return ResponseEntity.ok(loanService.listInstallmentsForLoanForCustomer(loanId, authenticatedCustomerId));
        }
    }

    @PostMapping("/{loanId}/pay")
    public ResponseEntity<PayLoanResponse> payLoan(@PathVariable Long loanId,
                                                   @Valid @RequestBody PayLoanRequest request,
                                                   Authentication authentication) {
        log.info("Received request to pay loan ID: {}. Amount: {}", loanId, request.getAmount());
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_" + Role.ADMIN.name()));
        if (isAdmin) {
            return ResponseEntity.ok(loanService.payLoan(loanId, request));
        } else {
            Long authenticatedCustomerId = getAuthenticatedCustomerId(authentication);
            return ResponseEntity.ok(loanService.payLoanForCustomer(loanId, request, authenticatedCustomerId));
        }
    }
}