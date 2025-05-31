package com.cagdasalagoz.creditmodulechallenge.banking.model.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class PayLoanRequest {
    @NotNull(message = "Payment amount cannot be null")
    @Positive(message = "Payment amount must be positive")
    private BigDecimal amount;
}