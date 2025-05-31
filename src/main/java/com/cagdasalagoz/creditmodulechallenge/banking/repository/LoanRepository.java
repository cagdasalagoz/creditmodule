package com.cagdasalagoz.creditmodulechallenge.banking.repository;

import com.cagdasalagoz.creditmodulechallenge.banking.dto.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loan, Long>, JpaSpecificationExecutor<Loan> {
    Optional<Loan> findByIdAndCustomerId(Long loanId, Long customerId);
    Boolean existsByIdAndCustomerId(Long customerId, Long loanId);
}