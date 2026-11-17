package com.pedrocmoreira.garage.os.infrastructure.persistence;

import com.pedrocmoreira.garage.os.domain.repository.SagaRepository;
import com.pedrocmoreira.garage.os.domain.saga.SagaInstance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SagaRepositoryAdapter implements SagaRepository {
    private final SagaJpaRepository jpa;

    @Override public SagaInstance save(SagaInstance saga) { return jpa.save(saga); }
    @Override public Optional<SagaInstance> findByServiceOrderId(Long serviceOrderId) { return jpa.findByServiceOrderId(serviceOrderId); }
}
