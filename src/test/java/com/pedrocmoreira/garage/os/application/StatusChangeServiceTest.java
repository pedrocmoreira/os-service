package com.pedrocmoreira.garage.os.application;

import com.pedrocmoreira.garage.os.TestFixtures;
import com.pedrocmoreira.garage.os.domain.model.ServiceOrder;
import com.pedrocmoreira.garage.os.domain.model.ServiceOrderHistory;
import com.pedrocmoreira.garage.os.domain.model.StatusSO;
import com.pedrocmoreira.garage.os.domain.repository.HistoryRepository;
import com.pedrocmoreira.garage.os.domain.repository.ServiceOrderRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StatusChangeServiceTest {
    private final ServiceOrderRepository repository = mock(ServiceOrderRepository.class);
    private final HistoryRepository historyRepository = mock(HistoryRepository.class);
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final StatusChangeService service = new StatusChangeService(repository, historyRepository, registry);

    @Test
    void shouldChangeStatusAndRecordHistory() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ServiceOrder so = TestFixtures.serviceOrder(StatusSO.RECEBIDA);

        service.change(so, StatusSO.EM_DIAGNOSTICO, "nota");

        ArgumentCaptor<ServiceOrderHistory> captor = ArgumentCaptor.forClass(ServiceOrderHistory.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getFromStatus()).isEqualTo(StatusSO.RECEBIDA);
        assertThat(captor.getValue().getToStatus()).isEqualTo(StatusSO.EM_DIAGNOSTICO);
        assertThat(registry.counter("garage.service_order.status.transitions", "to", "EM_DIAGNOSTICO").count()).isEqualTo(1);
    }

    @Test
    void shouldRecordCreation() {
        service.recordCreation(TestFixtures.serviceOrder(StatusSO.RECEBIDA));
        verify(historyRepository).save(any());
        assertThat(registry.counter("garage.service_order.created").count()).isEqualTo(1);
    }
}
