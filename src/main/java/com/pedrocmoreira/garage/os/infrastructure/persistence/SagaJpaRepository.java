package com.pedrocmoreira.garage.os.infrastructure.persistence;

import com.pedrocmoreira.garage.os.domain.saga.SagaInstance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SagaJpaRepository extends JpaRepository<SagaInstance, String> {
    Optional<SagaInstance> findByServiceOrderId(Long serviceOrderId);
}
