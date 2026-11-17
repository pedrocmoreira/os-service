package com.pedrocmoreira.garage.os.infrastructure.persistence;

import com.pedrocmoreira.garage.os.domain.model.ServiceOrderHistory;
import com.pedrocmoreira.garage.os.domain.repository.HistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class HistoryRepositoryAdapter implements HistoryRepository {
    private final HistoryJpaRepository jpa;

    @Override public ServiceOrderHistory save(ServiceOrderHistory history) { return jpa.save(history); }
    @Override public List<ServiceOrderHistory> findByServiceOrderId(Long serviceOrderId) {
        return jpa.findByServiceOrderIdOrderByChangedAtAsc(serviceOrderId);
    }
}
