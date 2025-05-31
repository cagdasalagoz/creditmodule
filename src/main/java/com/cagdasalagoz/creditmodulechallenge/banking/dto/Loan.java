package com.cagdasalagoz.creditmodulechallenge.banking.dto;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "loans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    @Column(nullable = false)
    private BigDecimal loanAmount;
    @Column(nullable = false)
    private BigDecimal totalLoanAmountWithInterest;
    @Column(nullable = false)
    private Integer numberOfInstallments;
    @Column(nullable = false)
    private BigDecimal interestRate;
    @Column(nullable = false)
    private LocalDate createDate;
    @Column(nullable = false)
    private boolean isPaid;

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OrderBy("dueDate ASC")
    private List<LoanInstallment> installments;
}