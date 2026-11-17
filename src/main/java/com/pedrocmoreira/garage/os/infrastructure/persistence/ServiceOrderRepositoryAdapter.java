package com.pedrocmoreira.garage.os.infrastructure.persistence;

import com.pedrocmoreira.garage.os.domain.model.ServiceOrder;
import com.pedrocmoreira.garage.os.domain.model.StatusSO;
import com.pedrocmoreira.garage.os.domain.repository.ServiceOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Year;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ServiceOrderRepositoryAdapter implements ServiceOrderRepository {
    private final ServiceOrderJpaRepository jpa;

    @Override public ServiceOrder save(ServiceOrder serviceOrder) { return jpa.save(serviceOrder); }
    @Override public Optional<ServiceOrder> findById(Long id) { return jpa.findById(id); }
    @Override public Optional<ServiceOrder> findByNumber(String number) { return jpa.findByNumber(number); }
    @Override public List<ServiceOrder> findAll() { return jpa.findAll(); }
    @Override public List<ServiceOrder> findByStatus(StatusSO status) { return jpa.findByStatus(status); }

    @Override
    public String nextNumber() {
        return String.format("OS-%d-%05d", Year.now().getValue(), jpa.nextSequence());
    }
}
