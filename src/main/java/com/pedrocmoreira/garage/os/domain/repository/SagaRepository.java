package com.pedrocmoreira.garage.os.domain.repository;

import com.pedrocmoreira.garage.os.domain.saga.SagaInstance;

import java.util.Optional;

public interface SagaRepository {
    SagaInstance save(SagaInstance saga);
    Optional<SagaInstance> findByServiceOrderId(Long serviceOrderId);
}
