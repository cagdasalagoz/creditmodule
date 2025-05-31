package com.cagdasalagoz.creditmodulechallenge.banking.model.response;

import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class CreateLoanResponse {
    private Long id;
    private Long customerId;
    private String customerName;
    private String customerSurname;
    private BigDecimal loanAmount;
    private BigDecimal totalLoanAmountWithInterest;
    private BigDecimal interestRate;
    private Integer numberOfInstallments;
    private LocalDate createDate;
    private boolean isPaid;
    private List<InstallmentResponse> installments;
}
