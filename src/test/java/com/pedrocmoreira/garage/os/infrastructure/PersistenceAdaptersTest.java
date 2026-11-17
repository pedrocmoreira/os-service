package com.pedrocmoreira.garage.os.infrastructure;

import com.pedrocmoreira.garage.os.TestFixtures;
import com.pedrocmoreira.garage.os.domain.model.ServiceOrder;
import com.pedrocmoreira.garage.os.domain.model.ServiceOrderHistory;
import com.pedrocmoreira.garage.os.domain.model.StatusSO;
import com.pedrocmoreira.garage.os.domain.saga.SagaInstance;
import com.pedrocmoreira.garage.os.infrastructure.persistence.*;
import org.junit.jupiter.api.Test;

import java.time.Year;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PersistenceAdaptersTest {

    @Test
    void serviceOrderAdapterDelegatesToJpa() {
        ServiceOrderJpaRepository jpa = mock(ServiceOrderJpaRepository.class);
        ServiceOrderRepositoryAdapter adapter = new ServiceOrderRepositoryAdapter(jpa);
        ServiceOrder so = TestFixtures.serviceOrder(StatusSO.RECEBIDA);
        when(jpa.save(so)).thenReturn(so);
        when(jpa.findById(1L)).thenReturn(Optional.of(so));
        when(jpa.findByNumber("N")).thenReturn(Optional.of(so));
        when(jpa.findAll()).thenReturn(List.of(so));
        when(jpa.findByStatus(StatusSO.RECEBIDA)).thenReturn(List.of(so));
        when(jpa.nextSequence()).thenReturn(7L);

        assertThat(adapter.save(so)).isSameAs(so);
        assertThat(adapter.findById(1L)).contains(so);
        assertThat(adapter.findByNumber("N")).contains(so);
        assertThat(adapter.findAll()).hasSize(1);
        assertThat(adapter.findByStatus(StatusSO.RECEBIDA)).hasSize(1);
        assertThat(adapter.nextNumber()).isEqualTo("OS-" + Year.now().getValue() + "-00007");
    }

    @Test
    void sagaAndHistoryAdaptersDelegateToJpa() {
        SagaJpaRepository sagaJpa = mock(SagaJpaRepository.class);
        SagaRepositoryAdapter sagaAdapter = new SagaRepositoryAdapter(sagaJpa);
        SagaInstance saga = SagaInstance.builder().serviceOrderId(1L).build();
        when(sagaJpa.save(saga)).thenReturn(saga);
        when(sagaJpa.findByServiceOrderId(1L)).thenReturn(Optional.of(saga));
        assertThat(sagaAdapter.save(saga)).isSameAs(saga);
        assertThat(sagaAdapter.findByServiceOrderId(1L)).contains(saga);

        HistoryJpaRepository historyJpa = mock(HistoryJpaRepository.class);
        HistoryRepositoryAdapter historyAdapter = new HistoryRepositoryAdapter(historyJpa);
        ServiceOrderHistory h = ServiceOrderHistory.builder().serviceOrderId(1L).toStatus(StatusSO.RECEBIDA).build();
        when(historyJpa.save(h)).thenReturn(h);
        when(historyJpa.findByServiceOrderIdOrderByChangedAtAsc(1L)).thenReturn(List.of(h));
        assertThat(historyAdapter.save(h)).isSameAs(h);
        assertThat(historyAdapter.findByServiceOrderId(1L)).containsExactly(h);
    }

    @Test
    void processedMessageAdapterGuaranteesIdempotency() {
        ProcessedMessageJpaRepository jpa = mock(ProcessedMessageJpaRepository.class);
        ProcessedMessageRepositoryAdapter adapter = new ProcessedMessageRepositoryAdapter(jpa);
        when(jpa.existsById("a")).thenReturn(false);
        when(jpa.existsById("b")).thenReturn(true);

        assertThat(adapter.registerIfNew("a")).isTrue();
        verify(jpa).save(any(ProcessedMessage.class));
        assertThat(adapter.registerIfNew("b")).isFalse();
        assertThat(adapter.registerIfNew(null)).isTrue();
    }
}
