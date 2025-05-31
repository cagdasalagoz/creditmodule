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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private LoanRepository loanRepository;
    @Mock
    private LoanInstallmentRepository loanInstallmentRepository;
    @Mock
    private LoanMapper loanMapper;
    @Mock
    private InstallmentMapper installmentMapper;

    @InjectMocks
    private LoanServiceImpl loanService;

    private Customer testCustomer;
    private CreateLoanRequest createLoanRequest;
    private Loan testLoan;
    private LoanInstallment testInstallment1;
    private LoanInstallment testInstallment2;
    private CreateLoanResponse createLoanResponse;

    @BeforeEach
    void setUp() {
        testCustomer = Customer.builder()
                .id(1L)
                .name("John")
                .surname("Doe")
                .creditLimit(new BigDecimal("10000.00"))
                .usedCreditLimit(new BigDecimal("0.00"))
                .build();

        createLoanRequest = CreateLoanRequest.builder()
                .customerId(1L)
                .amount(new BigDecimal("5000.00"))
                .interestRate(new BigDecimal("0.1"))
                .numberOfInstallments(12)
                .build();

        testLoan = Loan.builder()
                .id(100L)
                .customer(testCustomer)
                .loanAmount(new BigDecimal("5000.00"))
                .totalLoanAmountWithInterest(new BigDecimal("5500.00"))
                .numberOfInstallments(12)
                .interestRate(new BigDecimal("0.1"))
                .createDate(LocalDate.now())
                .isPaid(false)
                .build();

        // Create some sample installments for the loan
        testInstallment1 = LoanInstallment.builder()
                .id(200L)
                .loan(testLoan)
                .amount(new BigDecimal("458.33")) // 5500/12
                .paidAmount(BigDecimal.ZERO)
                .dueDate(LocalDate.now().plusMonths(1).withDayOfMonth(1))
                .isPaid(false)
                .build();

        testInstallment2 = LoanInstallment.builder()
                .id(201L)
                .loan(testLoan)
                .amount(new BigDecimal("458.33"))
                .paidAmount(BigDecimal.ZERO)
                .dueDate(LocalDate.now().plusMonths(2).withDayOfMonth(1))
                .isPaid(false)
                .build();

        testLoan.setInstallments(Arrays.asList(testInstallment1, testInstallment2));

        createLoanResponse = CreateLoanResponse.builder()
                .id(100L)
                .customerId(1L)
                .loanAmount(new BigDecimal("5000.00"))
                .totalLoanAmountWithInterest(new BigDecimal("5500.00"))
                .numberOfInstallments(12)
                .interestRate(new BigDecimal("0.1"))
                .createDate(LocalDate.now())
                .isPaid(false)
                .build();
    }

    @Test
    @DisplayName("should create loan successfully")
    void createLoan_success() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(loanRepository.save(any(Loan.class))).thenReturn(testLoan);
        when(loanMapper.toDto(any(Loan.class))).thenReturn(createLoanResponse);

        CreateLoanResponse response = loanService.createLoan(createLoanRequest);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(new BigDecimal("5000.00"), response.getLoanAmount());
        assertEquals(12, response.getNumberOfInstallments());

        // Verify customer's used credit limit is updated and saved
        // My blog post about MockitoArgumentCaptor from 2020
        // https://www.cagdasalagoz.com/capture-parameters-with-mockito-argumentcaptor/
        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository, times(1)).save(customerCaptor.capture());
        assertEquals(new BigDecimal("5000.00"), customerCaptor.getValue().getUsedCreditLimit());

        // Verify loan and its installments are saved
        ArgumentCaptor<Loan> loanCaptor = ArgumentCaptor.forClass(Loan.class);
        verify(loanRepository, times(1)).save(loanCaptor.capture());
        assertNotNull(loanCaptor.getValue().getInstallments());
        assertEquals(12, loanCaptor.getValue().getInstallments().size());
        assertEquals(testLoan.getCustomer().getId(), loanCaptor.getValue().getCustomer().getId());
        assertFalse(loanCaptor.getValue().isPaid());
    }

    @Test
    @DisplayName("should throw ValidationException for invalid number of installments")
    void createLoan_invalidInstallments() {
        createLoanRequest.setNumberOfInstallments(7);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> loanService.createLoan(createLoanRequest));

        assertEquals("Number of installments can only be 6, 9, 12, or 24", exception.getMessage());
        verifyNoInteractions(customerRepository, loanRepository, loanInstallmentRepository);
    }

    @Test
    @DisplayName("should throw ValidationException for invalid interest rate (too low)")
    void createLoan_invalidInterestRateTooLow() {
        createLoanRequest.setInterestRate(new BigDecimal("0.05")); // Too low

        ValidationException exception = assertThrows(ValidationException.class,
                () -> loanService.createLoan(createLoanRequest));

        assertEquals("Interest rate must be between 0.1 and 0.5", exception.getMessage());
        verifyNoInteractions(customerRepository, loanRepository, loanInstallmentRepository);
    }

    @Test
    @DisplayName("should throw ValidationException for invalid interest rate (too high)")
    void createLoan_invalidInterestRateTooHigh() {
        createLoanRequest.setInterestRate(new BigDecimal("0.55")); // Too high

        ValidationException exception = assertThrows(ValidationException.class,
                () -> loanService.createLoan(createLoanRequest));

        assertEquals("Interest rate must be between 0.1 and 0.5", exception.getMessage());
        verifyNoInteractions(customerRepository, loanRepository, loanInstallmentRepository);
    }

    @Test
    @DisplayName("should throw NotFoundException if customer does not exist")
    void createLoan_customerNotFound() {
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> loanService.createLoan(createLoanRequest));

        assertEquals("Customer do no exist customerId: 1", exception.getMessage());
        verifyNoMoreInteractions(customerRepository, loanRepository, loanInstallmentRepository); // No other calls
    }

    @Test
    @DisplayName("should throw ValidationException if customer has insufficient credit")
    void createLoan_insufficientCredit() {
        testCustomer.setCreditLimit(new BigDecimal("4000.00")); // Not enough credit
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> loanService.createLoan(createLoanRequest));

        assertEquals("Customer does not have enough credit limit. Available: 4000.00, Requested: 5000.00",
                exception.getMessage());
        verifyNoMoreInteractions(customerRepository, loanRepository, loanInstallmentRepository);
    }

    @Test
    @DisplayName("should list loans with all filters")
    void listLoans_withAllFilters() {
        when(loanRepository.findAll(any(Specification.class))).thenReturn(Collections.singletonList(testLoan));
        when(loanMapper.toDtoList(anyList())).thenReturn(Collections.singletonList(createLoanResponse));

        List<CreateLoanResponse> result = loanService.listLoans(1L, 12, false);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(createLoanResponse, result.get(0));

        verify(loanMapper, times(1)).toDtoList(anyList());
    }

    @Test
    @DisplayName("should list loans with no filters (all loans)")
    void listLoans_noFilters() {
        when(loanRepository.findAll(any(Specification.class))).thenReturn(Arrays.asList(testLoan));
        when(loanMapper.toDtoList(anyList())).thenReturn(Arrays.asList(createLoanResponse));

        List<CreateLoanResponse> result = loanService.listLoans(null, null, null);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        verify(loanMapper, times(1)).toDtoList(anyList());
    }

    @Test
    @DisplayName("should list loans for a specific customer with filters")
    void listLoansForCustomer_withFilters() {
        when(loanRepository.findAll(any(Specification.class))).thenReturn(Collections.singletonList(testLoan));
        when(loanMapper.toDtoList(anyList())).thenReturn(Collections.singletonList(createLoanResponse));

        List<CreateLoanResponse> result = loanService.listLoansForCustomer(1L, 12, false);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(createLoanResponse, result.get(0));

        verify(loanMapper, times(1)).toDtoList(anyList());
    }

    @Test
    @DisplayName("should get loan details successfully")
    void getLoanDetails_success() {
        when(loanRepository.findById(100L)).thenReturn(Optional.of(testLoan));
        when(loanMapper.toDto(testLoan)).thenReturn(createLoanResponse);

        CreateLoanResponse result = loanService.getLoanDetails(100L);

        assertNotNull(result);
        assertEquals(createLoanResponse, result);
        verify(loanMapper, times(1)).toDto(testLoan);
    }

    @Test
    @DisplayName("should throw NotFoundException if loan details not found")
    void getLoanDetails_notFound() {
        when(loanRepository.findById(100L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> loanService.getLoanDetails(100L));

        assertEquals("Loan not found with ID: 100", exception.getMessage());
        verifyNoInteractions(loanMapper);
    }

    @Test
    @DisplayName("should get loan details for specific customer successfully")
    void getLoanDetailsForCustomer_success() {
        when(loanRepository.findByIdAndCustomerId(100L, 1L)).thenReturn(Optional.of(testLoan));
        when(loanMapper.toDto(testLoan)).thenReturn(createLoanResponse);

        CreateLoanResponse result = loanService.getLoanDetailsForCustomer(100L, 1L);

        assertNotNull(result);
        assertEquals(createLoanResponse, result);
        verify(loanMapper, times(1)).toDto(testLoan);
    }

    @Test
    @DisplayName("should throw NotFoundException if loan details not found for customer")
    void getLoanDetailsForCustomer_notFound() {
        when(loanRepository.findByIdAndCustomerId(100L, 1L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> loanService.getLoanDetailsForCustomer(100L, 1L));

        assertEquals("Loan not found with ID: 100 for customer 1", exception.getMessage());
        verifyNoInteractions(loanMapper);
    }

    @Test
    @DisplayName("should list installments for a loan successfully")
    void listInstallmentsForLoan_success() {
        when(loanRepository.existsById(100L)).thenReturn(true);
        when(loanInstallmentRepository.findByLoanIdOrderByDueDateAsc(100L))
                .thenReturn(Arrays.asList(testInstallment1, testInstallment2));
        InstallmentResponse installmentResponse1 = InstallmentResponse.builder().id(200L).amount(new BigDecimal("458.33")).build();
        InstallmentResponse installmentResponse2 = InstallmentResponse.builder().id(201L).amount(new BigDecimal("458.33")).build();
        when(installmentMapper.toDtoList(anyList()))
                .thenReturn(Arrays.asList(installmentResponse1, installmentResponse2));

        List<InstallmentResponse> result = loanService.listInstallmentsForLoan(100L);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(200L, result.get(0).getId());
        assertEquals(201L, result.get(1).getId());
    }

    @Test
    @DisplayName("should throw NotFoundException if loan not found when listing installments")
    void listInstallmentsForLoan_loanNotFound() {
        when(loanRepository.existsById(100L)).thenReturn(false);

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> loanService.listInstallmentsForLoan(100L));

        assertEquals("Loan not found with ID: 100", exception.getMessage());
        verifyNoInteractions(loanInstallmentRepository, installmentMapper);
    }

    @Test
    @DisplayName("should list installments for a loan for a specific customer successfully")
    void listInstallmentsForLoanForCustomer_success() {
        when(loanRepository.findByIdAndCustomerId(100L, 1L)).thenReturn(Optional.of(testLoan));
        when(loanInstallmentRepository.findByLoanIdOrderByDueDateAsc(100L))
                .thenReturn(Arrays.asList(testInstallment1, testInstallment2));
        InstallmentResponse installmentResponse1 = InstallmentResponse.builder().id(200L).amount(new BigDecimal("458.33")).build();
        InstallmentResponse installmentResponse2 = InstallmentResponse.builder().id(201L).amount(new BigDecimal("458.33")).build();
        when(installmentMapper.toDtoList(anyList()))
                .thenReturn(Arrays.asList(installmentResponse1, installmentResponse2));

        List<InstallmentResponse> result = loanService.listInstallmentsForLoanForCustomer(100L, 1L);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("should throw NotFoundException if loan not found for customer when listing installments")
    void listInstallmentsForLoanForCustomer_loanNotFound() {
        when(loanRepository.findByIdAndCustomerId(100L, 1L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> loanService.listInstallmentsForLoanForCustomer(100L, 1L));

        assertEquals("Loan not found with ID: 100 for customer 1", exception.getMessage());
        verifyNoInteractions(loanInstallmentRepository, installmentMapper);
    }

    @Test
    @DisplayName("should pay loan successfully with exact amount (and natural early payment discount)")
    void payLoan_success_exactAmount() {
        PayLoanRequest payRequest = new PayLoanRequest(new BigDecimal("458.33"));
        testLoan.setInstallments(Arrays.asList(testInstallment1, testInstallment2)); // Make sure loan has installments
        when(loanRepository.findById(100L)).thenReturn(Optional.of(testLoan));
        // Ensure that testInstallment1 is considered 'due now' or 'due in the future' as per service logic
        when(loanInstallmentRepository.findByLoanIdAndIsPaidFalseAndDueDateBeforeOrderByDueDateAsc(eq(100L),
                any(LocalDate.class)))
                .thenReturn(Collections.singletonList(testInstallment1));
        when(loanInstallmentRepository.save(any(LoanInstallment.class))).thenReturn(testInstallment1);
        when(loanInstallmentRepository.findByLoanIdAndIsPaidFalseOrderByDueDateAsc(100L))
                .thenReturn(Collections.singletonList(testInstallment2));
        when(loanRepository.save(any())).thenReturn(testLoan);

        PayLoanResponse response = loanService.payLoan(100L, payRequest);

        assertNotNull(response);
        assertEquals(1, response.getInstallmentsPaidCount());
        assertEquals(new BigDecimal("444.58"), response.getTotalAmountSpent());
        assertFalse(response.isLoanPaidCompletely());
        assertEquals("Payment processed.", response.getMessage());

    }

    @Test
    @DisplayName("should pay loan successfully and mark loan as paid, updating customer credit")
    void payLoan_success_fullPayment() {
        PayLoanRequest payRequest = new PayLoanRequest(new BigDecimal("458.33"));
        testLoan.setInstallments(Arrays.asList(testInstallment1)); // Only one installment to pay
        when(loanRepository.findById(100L)).thenReturn(Optional.of(testLoan));
        when(loanInstallmentRepository.findByLoanIdAndIsPaidFalseAndDueDateBeforeOrderByDueDateAsc(eq(100L), any(LocalDate.class)))
                .thenReturn(Collections.singletonList(testInstallment1));
        when(loanInstallmentRepository.save(any(LoanInstallment.class))).thenReturn(testInstallment1);
        when(loanInstallmentRepository.findByLoanIdAndIsPaidFalseOrderByDueDateAsc(100L)).thenReturn(Collections.emptyList()); // All paid now
        when(loanRepository.save(any())).thenReturn(testLoan);

        testCustomer.setUsedCreditLimit(new BigDecimal("5000.00")); // Assume customer used credit
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer); // Mock saving customer

        PayLoanResponse response = loanService.payLoan(100L, payRequest);

        assertNotNull(response);
        assertEquals(1, response.getInstallmentsPaidCount());
        // Expect the discounted amount
        assertEquals(new BigDecimal("444.58"), response.getTotalAmountSpent());
        assertTrue(response.isLoanPaidCompletely());
        assertEquals("Payment processed.", response.getMessage());
        assertTrue(testLoan.isPaid());
    }

    @Test
    @DisplayName("should handle payment for loan already fully paid")
    void payLoan_alreadyPaid() {
        testLoan.setPaid(true); // Loan is already paid
        when(loanRepository.findById(100L)).thenReturn(Optional.of(testLoan));

        PayLoanResponse response = loanService.payLoan(100L, new PayLoanRequest(new BigDecimal("100.00")));

        assertNotNull(response);
        assertTrue(response.isLoanPaidCompletely());
        assertEquals(0, response.getInstallmentsPaidCount());
        assertEquals(BigDecimal.ZERO, response.getTotalAmountSpent());
        assertEquals("Loan is already fully paid.", response.getMessage());
    }

    @Test
    @DisplayName("should apply discount for early payment")
    void payLoan_earlyPaymentDiscount() {
        PayLoanRequest payRequest = new PayLoanRequest(new BigDecimal("458.33")); // Sufficient to cover after discount
        testInstallment1.setDueDate(LocalDate.now().plusDays(30)); // Due in the future (early payment)
        testLoan.setInstallments(Arrays.asList(testInstallment1));

        when(loanRepository.findById(100L)).thenReturn(Optional.of(testLoan));
        when(loanInstallmentRepository.findByLoanIdAndIsPaidFalseAndDueDateBeforeOrderByDueDateAsc(eq(100L), any(LocalDate.class)))
                .thenReturn(Collections.singletonList(testInstallment1));
        when(loanInstallmentRepository.save(any(LoanInstallment.class))).thenReturn(testInstallment1);
        when(loanInstallmentRepository.findByLoanIdAndIsPaidFalseOrderByDueDateAsc(100L)).thenReturn(Collections.emptyList());
        when(loanRepository.save(any(Loan.class))).thenReturn(testLoan);

        testCustomer.setUsedCreditLimit(new BigDecimal("5000.00"));
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

        PayLoanResponse response = loanService.payLoan(100L, payRequest);

        assertNotNull(response);
        assertEquals(1, response.getInstallmentsPaidCount());
        // expected discounted amount: 458.33 - (458.33 * 0.001 * 30) = 458.33 - 13.7499 = 444.5801 -> 444.58
        assertEquals(new BigDecimal("444.58"), response.getTotalAmountSpent());
        assertTrue(response.isLoanPaidCompletely());
    }

    @Test
    @DisplayName("should apply penalty for late payment")
    void payLoan_latePaymentPenalty() {
        PayLoanRequest payRequest = new PayLoanRequest(new BigDecimal("500.00"));

        testInstallment1.setDueDate(LocalDate.now().minusDays(30)); // late payment
        testLoan.setInstallments(Arrays.asList(testInstallment1));

        when(loanRepository.findById(100L)).thenReturn(Optional.of(testLoan));

        when(loanInstallmentRepository.findByLoanIdAndIsPaidFalseAndDueDateBeforeOrderByDueDateAsc(eq(100L), any(LocalDate.class)))
                .thenReturn(Collections.singletonList(testInstallment1));

        when(loanInstallmentRepository.save(any(LoanInstallment.class))).thenReturn(testInstallment1);
        when(loanInstallmentRepository.findByLoanIdAndIsPaidFalseOrderByDueDateAsc(100L)).thenReturn(Collections.emptyList());
        when(loanRepository.save(any())).thenReturn(testLoan);

        testCustomer.setUsedCreditLimit(new BigDecimal("5000.00"));
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

        PayLoanResponse response = loanService.payLoan(100L, payRequest);

        assertNotNull(response);
        assertEquals(1, response.getInstallmentsPaidCount());

        assertEquals(new BigDecimal("472.08"), response.getTotalAmountSpent());
        assertTrue(response.isLoanPaidCompletely());
    }

    @Test
    @DisplayName("should not pay if insufficient amount for an installment")
    void payLoan_insufficientAmountForInstallment() {
        PayLoanRequest payRequest = new PayLoanRequest(new BigDecimal("100.00")); // Not enough for 458.33
        testLoan.setInstallments(Arrays.asList(testInstallment1, testInstallment2)); // Make sure loan has installments
        when(loanRepository.findById(100L)).thenReturn(Optional.of(testLoan));

        when(loanInstallmentRepository.findByLoanIdAndIsPaidFalseAndDueDateBeforeOrderByDueDateAsc(eq(100L),
                any(LocalDate.class)))
                .thenReturn(Collections.singletonList(testInstallment1));
        when(loanInstallmentRepository.findByLoanIdAndIsPaidFalseOrderByDueDateAsc(100L))
                .thenReturn(Arrays.asList(testInstallment1, testInstallment2));

        PayLoanResponse response = loanService.payLoan(100L, payRequest);

        assertNotNull(response);
        assertEquals(0, response.getInstallmentsPaidCount());
        assertEquals(BigDecimal.ZERO, response.getTotalAmountSpent());
        assertFalse(response.isLoanPaidCompletely());
        assertEquals("No installments could be paid with the provided amount or due to restrictions.", response.getMessage());
    }

    @Test
    @DisplayName("should throw PaymentException if no payable installments found")
    void payLoan_noPayableInstallments() {
        PayLoanRequest payRequest = new PayLoanRequest(new BigDecimal("100.00"));
        when(loanRepository.findById(100L)).thenReturn(Optional.of(testLoan));
        when(loanInstallmentRepository.findByLoanIdAndIsPaidFalseAndDueDateBeforeOrderByDueDateAsc(eq(100L), any(LocalDate.class)))
                .thenReturn(Collections.emptyList()); // No unpaid installments due now

        PaymentException exception = assertThrows(PaymentException.class,
                () -> loanService.payLoan(100L, payRequest));

        assertEquals("No payable installments found for loan ID: 100", exception.getMessage());
    }

    @Test
    @DisplayName("should handle payLoanForCustomer when loan does not belong to customer")
    void payLoanForCustomer_loanDoesNotBelongToCustomer() {
        PayLoanRequest payRequest = new PayLoanRequest(new BigDecimal("100.00"));
        Long wrongCustomerId = 99L;

        when(loanRepository.existsByIdAndCustomerId(100L, wrongCustomerId)).thenReturn(false);

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> loanService.payLoanForCustomer(100L, payRequest, wrongCustomerId));

        assertEquals("Loan not found with ID: 100 for customer 99", exception.getMessage());
    }

    @Test
    @DisplayName("should correctly verify loan ownership for customer before processing payment")
    void payLoanForCustomer_successfulVerification() {
        PayLoanRequest payRequest = new PayLoanRequest(new BigDecimal("458.33"));
        testLoan.setInstallments(Arrays.asList(testInstallment1)); // Only one installment to pay
        testLoan.setPaid(false);
        testCustomer.setUsedCreditLimit(new BigDecimal("5000.00"));

        when(loanRepository.existsByIdAndCustomerId(100L, 1L)).thenReturn(true);
        when(loanRepository.findById(100L)).thenReturn(Optional.of(testLoan));
        when(loanInstallmentRepository.findByLoanIdAndIsPaidFalseAndDueDateBeforeOrderByDueDateAsc(eq(100L), any(LocalDate.class)))
                .thenReturn(Collections.singletonList(testInstallment1));
        when(loanInstallmentRepository.save(any(LoanInstallment.class))).thenReturn(testInstallment1);
        when(loanInstallmentRepository.findByLoanIdAndIsPaidFalseOrderByDueDateAsc(100L)).thenReturn(Collections.emptyList());
        when(loanRepository.save(any(Loan.class))).thenReturn(testLoan);
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

        PayLoanResponse response = loanService.payLoanForCustomer(100L, payRequest, 1L);

        assertNotNull(response);
        assertEquals(1, response.getInstallmentsPaidCount());
        assertTrue(response.isLoanPaidCompletely());
    }
}