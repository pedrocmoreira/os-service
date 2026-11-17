package com.pedrocmoreira.garage.os.infrastructure.persistence;

import com.pedrocmoreira.garage.os.domain.model.ServiceOrderHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoryJpaRepository extends JpaRepository<ServiceOrderHistory, Long> {
    List<ServiceOrderHistory> findByServiceOrderIdOrderByChangedAtAsc(Long serviceOrderId);
}
