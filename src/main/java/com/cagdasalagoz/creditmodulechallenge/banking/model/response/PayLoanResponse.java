package com.cagdasalagoz.creditmodulechallenge.banking.model.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class PayLoanResponse {
    private int installmentsPaidCount;
    private BigDecimal totalAmountSpent;
    private boolean loanPaidCompletely;
    private String message;
}