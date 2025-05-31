package com.cagdasalagoz.creditmodulechallenge.banking.service;

import com.cagdasalagoz.creditmodulechallenge.banking.model.request.CreateLoanRequest;
import com.cagdasalagoz.creditmodulechallenge.banking.model.request.PayLoanRequest;
import com.cagdasalagoz.creditmodulechallenge.banking.model.response.CreateLoanResponse;
import com.cagdasalagoz.creditmodulechallenge.banking.model.response.InstallmentResponse;
import com.cagdasalagoz.creditmodulechallenge.banking.model.response.PayLoanResponse;


import java.util.List;

public interface LoanService {
    CreateLoanResponse createLoan(CreateLoanRequest request);
    List<CreateLoanResponse> listLoans(Long customerId, Integer numberOfInstallments, Boolean isPaid);
    CreateLoanResponse getLoanDetails(Long loanId);
    List<InstallmentResponse> listInstallmentsForLoan(Long loanId);
    PayLoanResponse payLoan(Long loanId, PayLoanRequest request);
    CreateLoanResponse getLoanDetailsForCustomer(Long loanId, Long customerId);
    List<InstallmentResponse> listInstallmentsForLoanForCustomer(Long loanId, Long customerId);
    PayLoanResponse payLoanForCustomer(Long loanId, PayLoanRequest request, Long customerId);
    List<CreateLoanResponse> listLoansForCustomer(Long customerId, Integer numberOfInstallments, Boolean isPaid);
}
