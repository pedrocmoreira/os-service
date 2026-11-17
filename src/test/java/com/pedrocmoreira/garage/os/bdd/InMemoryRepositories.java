package com.pedrocmoreira.garage.os.bdd;

import com.pedrocmoreira.garage.os.domain.model.ServiceOrder;
import com.pedrocmoreira.garage.os.domain.model.ServiceOrderHistory;
import com.pedrocmoreira.garage.os.domain.model.StatusSO;
import com.pedrocmoreira.garage.os.domain.port.MessagePublisher;
import com.pedrocmoreira.garage.os.domain.repository.*;
import com.pedrocmoreira.garage.os.domain.saga.SagaInstance;
import com.pedrocmoreira.garage.os.domain.saga.SagaMessage;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/** Implementações em memória das portas, permitindo exercitar o fluxo completo sem infraestrutura. */
final class InMemoryRepositories {
    private InMemoryRepositories() {}

    static class Orders implements ServiceOrderRepository {
        final Map<Long, ServiceOrder> data = new HashMap<>();
        final AtomicLong seq = new AtomicLong();

        public ServiceOrder save(ServiceOrder so) {
            if (so.getId() == null) so.setId(seq.incrementAndGet());
            data.put(so.getId(), so);
            return so;
        }
        public Optional<ServiceOrder> findById(Long id) { return Optional.ofNullable(data.get(id)); }
        public Optional<ServiceOrder> findByNumber(String n) { return data.values().stream().filter(s -> s.getNumber().equals(n)).findFirst(); }
        public List<ServiceOrder> findAll() { return new ArrayList<>(data.values()); }
        public List<ServiceOrder> findByStatus(StatusSO st) { return data.values().stream().filter(s -> s.getStatus() == st).toList(); }
        public String nextNumber() { return String.format("OS-2026-%05d", seq.get() + 1); }
    }

    static class Sagas implements SagaRepository {
        final Map<Long, SagaInstance> data = new HashMap<>();
        public SagaInstance save(SagaInstance s) { data.put(s.getServiceOrderId(), s); return s; }
        public Optional<SagaInstance> findByServiceOrderId(Long id) { return Optional.ofNullable(data.get(id)); }
    }

    static class History implements HistoryRepository {
        final List<ServiceOrderHistory> data = new ArrayList<>();
        public ServiceOrderHistory save(ServiceOrderHistory h) { data.add(h); return h; }
        public List<ServiceOrderHistory> findByServiceOrderId(Long id) { return data.stream().filter(h -> h.getServiceOrderId().equals(id)).toList(); }
    }

    static class Processed implements ProcessedMessageRepository {
        final Set<String> ids = new HashSet<>();
        public boolean registerIfNew(String id) { return ids.add(id); }
    }

    static class RecordingPublisher implements MessagePublisher {
        final List<SagaMessage> messages = new ArrayList<>();
        public void publish(SagaMessage m) { messages.add(m); }
        List<String> types() { return messages.stream().map(SagaMessage::type).toList(); }
    }
}
