package com.pedrocmoreira.garage.os.infrastructure.persistence;

import com.pedrocmoreira.garage.os.domain.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerJpaRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByDocument(String document);
    boolean existsByDocument(String document);
}
