package com.pedrocmoreira.garage.os.application;

import com.pedrocmoreira.garage.os.domain.model.ServiceOrder;
import com.pedrocmoreira.garage.os.domain.model.ServiceOrderHistory;
import com.pedrocmoreira.garage.os.domain.model.StatusSO;
import com.pedrocmoreira.garage.os.domain.repository.HistoryRepository;
import com.pedrocmoreira.garage.os.domain.repository.ServiceOrderRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Aplica transições de status na OS registrando histórico e métricas. */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatusChangeService {
    private final ServiceOrderRepository serviceOrderRepository;
    private final HistoryRepository historyRepository;
    private final MeterRegistry meterRegistry;

    public ServiceOrder change(ServiceOrder serviceOrder, StatusSO newStatus, String note) {
        StatusSO previous = serviceOrder.changeStatus(newStatus);
        ServiceOrder saved = serviceOrderRepository.save(serviceOrder);
        historyRepository.save(ServiceOrderHistory.builder()
                .serviceOrderId(saved.getId())
                .fromStatus(previous)
                .toStatus(newStatus)
                .note(note)
                .build());
        meterRegistry.counter("garage.service_order.status.transitions", "to", newStatus.name()).increment();
        log.info("OS {}: {} -> {} ({})", saved.getNumber(), previous, newStatus, note);
        return saved;
    }

    public void recordCreation(ServiceOrder serviceOrder) {
        historyRepository.save(ServiceOrderHistory.builder()
                .serviceOrderId(serviceOrder.getId())
                .toStatus(serviceOrder.getStatus())
                .note("Ordem de serviço aberta")
                .build());
        meterRegistry.counter("garage.service_order.created").increment();
    }
}
