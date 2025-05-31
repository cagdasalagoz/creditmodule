package com.cagdasalagoz.creditmodulechallenge.banking.repository;

import com.cagdasalagoz.creditmodulechallenge.banking.dto.Customer;
import com.cagdasalagoz.creditmodulechallenge.banking.dto.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
