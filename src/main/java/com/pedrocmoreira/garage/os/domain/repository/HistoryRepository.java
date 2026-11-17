package com.pedrocmoreira.garage.os.domain.repository;

import com.pedrocmoreira.garage.os.domain.model.ServiceOrderHistory;

import java.util.List;

public interface HistoryRepository {
    ServiceOrderHistory save(ServiceOrderHistory history);
    List<ServiceOrderHistory> findByServiceOrderId(Long serviceOrderId);
}
