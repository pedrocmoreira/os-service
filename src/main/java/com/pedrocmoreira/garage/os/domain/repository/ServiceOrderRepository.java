package com.pedrocmoreira.garage.os.domain.repository;

import com.pedrocmoreira.garage.os.domain.model.ServiceOrder;
import com.pedrocmoreira.garage.os.domain.model.StatusSO;

import java.util.List;
import java.util.Optional;

public interface ServiceOrderRepository {
    ServiceOrder save(ServiceOrder serviceOrder);
    Optional<ServiceOrder> findById(Long id);
    Optional<ServiceOrder> findByNumber(String number);
    List<ServiceOrder> findAll();
    List<ServiceOrder> findByStatus(StatusSO status);
    String nextNumber();
}
