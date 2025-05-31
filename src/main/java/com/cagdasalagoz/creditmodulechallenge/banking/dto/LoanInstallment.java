package com.cagdasalagoz.creditmodulechallenge.banking.dto;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "loan_installments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanInstallment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;
    @Column(nullable = false)
    private BigDecimal amount;
    @Column(nullable = false)
    private BigDecimal paidAmount;
    @Column(nullable = false)
    private LocalDate dueDate;
    private LocalDate paymentDate;
    @Column(nullable = false)
    private boolean isPaid;
}