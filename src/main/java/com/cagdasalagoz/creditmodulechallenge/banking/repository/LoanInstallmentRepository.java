package com.cagdasalagoz.creditmodulechallenge.banking.repository;

import com.cagdasalagoz.creditmodulechallenge.banking.dto.LoanInstallment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.time.LocalDate;

public interface LoanInstallmentRepository extends JpaRepository<LoanInstallment, Long> {
    List<LoanInstallment> findByLoanIdOrderByDueDateAsc(Long loanId);
    List<LoanInstallment> findByLoanIdAndIsPaidFalseOrderByDueDateAsc(Long loanId);
    // find installments for a loan, not paid, ordered by due date, within three months into the future
    List<LoanInstallment> findByLoanIdAndIsPaidFalseAndDueDateBeforeOrderByDueDateAsc(Long loanId,
                                                                                      LocalDate maxPayableDueDate);
}