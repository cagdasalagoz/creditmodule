package com.cagdasalagoz.creditmodulechallenge.banking.repository;

import com.cagdasalagoz.creditmodulechallenge.banking.dto.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByUser_Username(String username);
}
