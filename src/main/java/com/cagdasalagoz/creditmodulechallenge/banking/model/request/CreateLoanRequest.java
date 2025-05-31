package com.cagdasalagoz.creditmodulechallenge.banking.model.request;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class CreateLoanRequest {
    @NotNull(message = "Customer ID cannot be null")
    private Long customerId;

    @NotNull(message = "Loan amount cannot be null")
    @Positive(message = "Loan amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "Interest rate cannot be null")
    @DecimalMin(value = "0.1", message = "Interest rate must be at least 0.1")
    @DecimalMax(value = "0.5", message = "Interest rate must be at most 0.5")
    private BigDecimal interestRate;

    @NotNull(message = "Number of installments cannot be null")
    private Integer numberOfInstallments;
}
