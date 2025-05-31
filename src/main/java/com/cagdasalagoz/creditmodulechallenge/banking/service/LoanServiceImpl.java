package com.cagdasalagoz.creditmodulechallenge.banking.service;

import com.cagdasalagoz.creditmodulechallenge.banking.dto.Customer;
import com.cagdasalagoz.creditmodulechallenge.banking.dto.Loan;
import com.cagdasalagoz.creditmodulechallenge.banking.dto.LoanInstallment;
import com.cagdasalagoz.creditmodulechallenge.banking.exception.NotFoundException;
import com.cagdasalagoz.creditmodulechallenge.banking.exception.PaymentException;
import com.cagdasalagoz.creditmodulechallenge.banking.exception.ValidationException;
import com.cagdasalagoz.creditmodulechallenge.banking.model.request.CreateLoanRequest;
import com.cagdasalagoz.creditmodulechallenge.banking.model.request.PayLoanRequest;
import com.cagdasalagoz.creditmodulechallenge.banking.model.response.CreateLoanResponse;
import com.cagdasalagoz.creditmodulechallenge.banking.model.response.InstallmentResponse;
import com.cagdasalagoz.creditmodulechallenge.banking.model.response.PayLoanResponse;
import com.cagdasalagoz.creditmodulechallenge.banking.repository.CustomerRepository;
import com.cagdasalagoz.creditmodulechallenge.banking.repository.LoanInstallmentRepository;
import com.cagdasalagoz.creditmodulechallenge.banking.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Predicate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanServiceImpl implements LoanService {

    private final CustomerRepository customerRepository;
    private final LoanRepository loanRepository;
    private final LoanInstallmentRepository loanInstallmentRepository;
    private final LoanMapper loanMapper;
    private final InstallmentMapper installmentMapper;

    private static final Set<Integer> ALLOWED_INSTALLMENT_NUMBERS = Set.of(6, 9, 12, 24);
    private static final BigDecimal MIN_INTEREST_RATE = new BigDecimal("0.1");
    private static final BigDecimal MAX_INTEREST_RATE = new BigDecimal("0.5");
    private static final BigDecimal DAILY_PENALTY_DISCOUNT_RATE = new BigDecimal("0.001");

    @Override
    @Transactional
    public CreateLoanResponse createLoan(CreateLoanRequest request) {
        log.info("Attempting to create loan for customer ID: {}", request.getCustomerId());

        // Validate number of installments
        if (!ALLOWED_INSTALLMENT_NUMBERS.contains(request.getNumberOfInstallments())) {
            throw new ValidationException("Number of installments can only be 6, 9, 12, or 24");
        }

        // Validate interest rate. We don't actually need this because we have jakarta validation on the request object
        // but good to have just as a safety net.
        if (request.getInterestRate().compareTo(MIN_INTEREST_RATE) < 0
                || request.getInterestRate().compareTo(MAX_INTEREST_RATE) > 0) {
            log.warn("Admin tried to create loan with a non allowed interest rate: {} for customerId: {}",
                    request.getInterestRate(), request.getCustomerId());
            throw new ValidationException("Interest rate must be between 0.1 and 0.5");
        }

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new NotFoundException("Customer do no exist customerId: " + request.getCustomerId()));

        BigDecimal availableCredit = customer.getCreditLimit().subtract(customer.getUsedCreditLimit());
        if (availableCredit.compareTo(request.getAmount()) < 0) {
            throw new ValidationException("Customer does not have enough credit limit. Available: " + availableCredit
                    + ", Requested: " + request.getAmount());
        }

        // Calculate total loan amount and installment amount
        BigDecimal totalLoanAmountWithInterest = request.getAmount()
                .multiply(BigDecimal.ONE.add(request.getInterestRate()));
        BigDecimal installmentAmount = totalLoanAmountWithInterest.divide(
                BigDecimal.valueOf(request.getNumberOfInstallments()), 2, RoundingMode.HALF_UP);

        Loan loan = Loan.builder()
                .customer(customer)
                .loanAmount(request.getAmount())
                .totalLoanAmountWithInterest(totalLoanAmountWithInterest)
                .numberOfInstallments(request.getNumberOfInstallments())
                .interestRate(request.getInterestRate())
                .createDate(LocalDate.now())
                .isPaid(false)
                .build();

        List<LoanInstallment> installments = new ArrayList<>();
        LocalDate firstDueDate = LocalDate.now().plusMonths(1).withDayOfMonth(1);
        for (int i = 0; i < request.getNumberOfInstallments(); i++) {
            installments.add(LoanInstallment.builder()
                    .loan(loan)
                    .amount(installmentAmount)
                    .paidAmount(BigDecimal.ZERO)
                    .dueDate(firstDueDate.plusMonths(i))
                    .isPaid(false)
                    .build());
        }
        loan.setInstallments(installments);

        // Update customer's used credit limit
        customer.setUsedCreditLimit(customer.getUsedCreditLimit().add(request.getAmount()));
        customerRepository.save(customer);

        Loan savedLoan = loanRepository.save(loan);
        log.info("Loan created successfully with ID: {}", savedLoan.getId());
        return loanMapper.toDto(savedLoan);
    }

    @Override
    public List<CreateLoanResponse> listLoans(Long customerId, Integer numberOfInstallments, Boolean isPaid) {
        log.info("Listing loans for customer ID: {}, installments: {}, isPaid: {}", customerId, numberOfInstallments, isPaid);
        Specification<Loan> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (customerId != null) {
                predicates.add(criteriaBuilder.equal(root.get("customer").get("id"), customerId));
            }
            if (numberOfInstallments != null) {
                predicates.add(criteriaBuilder.equal(root.get("numberOfInstallments"), numberOfInstallments));
            }
            if (isPaid != null) {
                predicates.add(criteriaBuilder.equal(root.get("isPaid"), isPaid));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        return loanMapper.toDtoList(loanRepository.findAll(spec));
    }

    @Override
    public List<CreateLoanResponse> listLoansForCustomer(Long customerId, Integer numberOfInstallments, Boolean isPaid) {
        // This method ensures we only list loans for the given customerId.
        // The controller will ensure this customerId matches the authenticated user if role is CUSTOMER.
        log.info("Listing loans for specific customer ID: {}, installments: {}, isPaid: {}", customerId,
                 numberOfInstallments, isPaid);
        Specification<Loan> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            // filter by the provided customerId
            predicates.add(criteriaBuilder.equal(root.get("customer").get("id"), customerId));

            if (numberOfInstallments != null) {
                predicates.add(criteriaBuilder.equal(root.get("numberOfInstallments"), numberOfInstallments));
            }
            if (isPaid != null) {
                predicates.add(criteriaBuilder.equal(root.get("isPaid"), isPaid));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        return loanMapper.toDtoList(loanRepository.findAll(spec));
    }


    @Override
    public CreateLoanResponse getLoanDetails(Long loanId) {
        log.info("Fetching details for loan ID: {}", loanId);
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new NotFoundException("Loan not found with ID: " + loanId));
        return loanMapper.toDto(loan);
    }

    @Override
    public CreateLoanResponse getLoanDetailsForCustomer(Long loanId, Long customerId) {
        log.info("Fetching details for loan ID: {} for customer ID: {}", loanId, customerId);
        Loan loan = loanRepository.findByIdAndCustomerId(loanId, customerId)
                .orElseThrow(() -> new NotFoundException("Loan not found with ID: " + loanId + " for customer " + customerId));
        return loanMapper.toDto(loan);
    }

    @Override
    public List<InstallmentResponse> listInstallmentsForLoan(Long loanId) {
        log.info("Listing installments for loan ID: {}", loanId);
        if (!loanRepository.existsById(loanId)) {
            throw new NotFoundException("Loan not found with ID: " + loanId);
        }
        List<LoanInstallment> installments = loanInstallmentRepository.findByLoanIdOrderByDueDateAsc(loanId);
        return installmentMapper.toDtoList(installments);
    }

    @Override
    public List<InstallmentResponse> listInstallmentsForLoanForCustomer(Long loanId, Long customerId) {
        log.info("Listing installments for loan ID: {} for customer ID: {}", loanId, customerId);
        Loan loan = loanRepository.findByIdAndCustomerId(loanId, customerId)
                .orElseThrow(() -> new NotFoundException("Loan not found with ID: " + loanId +
                        " for customer " + customerId));
        List<LoanInstallment> installments = loanInstallmentRepository.findByLoanIdOrderByDueDateAsc(loan.getId());
        return installmentMapper.toDtoList(installments);
    }


    @Override
    @Transactional
    public PayLoanResponse payLoan(Long loanId, PayLoanRequest request) {
        return processPayment(loanId, request.getAmount(), null);
    }

    @Override
    @Transactional
    public PayLoanResponse payLoanForCustomer(Long loanId, PayLoanRequest request, Long customerId) {
        // Ensure the loan belongs to the customer before processing payment
        if (!loanRepository.existsByIdAndCustomerId(loanId, customerId)) {
            throw new NotFoundException("Loan not found with ID: " + loanId + " for customer " + customerId);
        }
        return processPayment(loanId, request.getAmount(), customerId);
    }

    private PayLoanResponse processPayment(Long loanId, BigDecimal paymentAmount, Long customerIdForVerification) {
        log.info("Processing payment for loan ID: {}. Amount: {}", loanId, paymentAmount);
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new NotFoundException("Loan not found with ID: " + loanId));

        if (customerIdForVerification != null && !loan.getCustomer().getId().equals(customerIdForVerification)) {
            throw new ValidationException("Loan " + loanId + " does not belong to customer " + customerIdForVerification);
        }

        if (loan.isPaid()) {
            return PayLoanResponse.builder()
                    .installmentsPaidCount(0)
                    .totalAmountSpent(BigDecimal.ZERO)
                    .loanPaidCompletely(true)
                    .message("Loan is already fully paid.")
                    .build();
        }

        // Cannot pay installments due more than 3 calendar months from now.
        LocalDate maxPayableDueDate = LocalDate.now().plusMonths(3).withDayOfMonth(1);

        List<LoanInstallment> unpaidInstallments = loanInstallmentRepository
                .findByLoanIdAndIsPaidFalseAndDueDateBeforeOrderByDueDateAsc(loanId, maxPayableDueDate);

        if (unpaidInstallments.isEmpty()) {
            throw new PaymentException("No payable installments found for loan ID: " + loanId);
        }

        BigDecimal remainingPaymentAmount = paymentAmount;
        int installmentsPaidCount = 0;
        BigDecimal totalAmountActuallySpentOnInstallments = BigDecimal.ZERO;

        for (LoanInstallment installment : unpaidInstallments) {
            BigDecimal actualInstallmentAmountToPay = installment.getAmount();

            // Apply discount or penalty
            long daysDifference = ChronoUnit.DAYS.between(installment.getDueDate(), LocalDate.now());
            BigDecimal adjustmentFactor;

            if (daysDifference < 0) { // Paid before due date (discount)
                daysDifference *= -1;
                adjustmentFactor = installment.getAmount()
                        .multiply(DAILY_PENALTY_DISCOUNT_RATE)
                        .multiply(BigDecimal.valueOf(daysDifference));
                actualInstallmentAmountToPay = installment.getAmount().subtract(adjustmentFactor);
                log.debug("Applying discount of {} for early payment of installment ID: {}", adjustmentFactor, installment.getId());
            } else if (daysDifference > 0) {
                // Paid after due date (penalty)
                adjustmentFactor = installment.getAmount()
                        .multiply(DAILY_PENALTY_DISCOUNT_RATE)
                        .multiply(BigDecimal.valueOf(daysDifference));
                actualInstallmentAmountToPay = installment.getAmount().add(adjustmentFactor);
                log.debug("Applying penalty of {} for late payment of installment ID: {}", adjustmentFactor, installment.getId());
            }

            // Verify amount is not negative after discount
            actualInstallmentAmountToPay = actualInstallmentAmountToPay.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

            if (remainingPaymentAmount.compareTo(actualInstallmentAmountToPay) >= 0) {
                installment.setPaid(true);
                installment.setPaymentDate(LocalDate.now());
                installment.setPaidAmount(actualInstallmentAmountToPay); // Store the actual amount paid
                loanInstallmentRepository.save(installment);

                remainingPaymentAmount = remainingPaymentAmount.subtract(actualInstallmentAmountToPay);
                totalAmountActuallySpentOnInstallments = totalAmountActuallySpentOnInstallments.add(actualInstallmentAmountToPay);
                installmentsPaidCount++;
                log.info("Paid installment ID: {}. Amount: {}. Remaining payment: {}", installment.getId(),
                        actualInstallmentAmountToPay, remainingPaymentAmount);
            } else {
                // Not enough money to pay this installment
                log.info("Not enough remaining payment ({}) to cover installment ID: {} (amount: {})",
                        remainingPaymentAmount, installment.getId(), actualInstallmentAmountToPay);
                break;
            }
        }

        // Check if all installments for the loan are now paid
        boolean allInstallmentsPaid = loanInstallmentRepository.findByLoanIdAndIsPaidFalseOrderByDueDateAsc(loanId).isEmpty();
        if (allInstallmentsPaid) {
            loan.setPaid(true);
            // Update customer's used credit limit
            Customer customer = loan.getCustomer();
            customer.setUsedCreditLimit(customer.getUsedCreditLimit().subtract(loan.getLoanAmount()));
            // Ensure usedCreditLimit doesn't go below zero
            customer.setUsedCreditLimit(customer.getUsedCreditLimit().max(BigDecimal.ZERO));
            customerRepository.save(customer);
            log.info("Loan ID: {} is now fully paid. Customer {} used credit limit updated.", loanId, customer.getId());
        }
        loanRepository.save(loan); // Save loan status (isPaid)

        return PayLoanResponse.builder()
                .installmentsPaidCount(installmentsPaidCount)
                .totalAmountSpent(totalAmountActuallySpentOnInstallments)
                .loanPaidCompletely(loan.isPaid())
                .message(installmentsPaidCount > 0 ? "Payment processed." :
                        "No installments could be paid with the provided amount or due to restrictions.")
                .build();
    }
}