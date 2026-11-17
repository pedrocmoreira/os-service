package com.pedrocmoreira.garage.os.infrastructure;

import com.pedrocmoreira.garage.os.application.ServiceOrderSagaOrchestrator;
import com.pedrocmoreira.garage.os.domain.saga.MessageTypes;
import com.pedrocmoreira.garage.os.domain.saga.SagaMessage;
import com.pedrocmoreira.garage.os.infrastructure.messaging.RabbitMessagePublisher;
import com.pedrocmoreira.garage.os.infrastructure.messaging.SagaEventListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MessagingTest {
    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final RabbitMessagePublisher publisher = new RabbitMessagePublisher(rabbitTemplate);
    private final SagaMessage message = SagaMessage.of(MessageTypes.CMD_EXECUTION_CANCEL, 1L, "OS-1", Map.of());

    @AfterEach
    void clear() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void publishesImmediatelyWithoutTransaction() {
        publisher.publish(message);
        verify(rabbitTemplate).convertAndSend(eq(MessageTypes.EXCHANGE), eq(message.type()), eq(message), any(MessagePostProcessor.class));
    }

    @Test
    void publishesOnlyAfterCommitInsideTransaction() {
        TransactionSynchronizationManager.initSynchronization();
        publisher.publish(message);
        verifyNoInteractions(rabbitTemplate);

        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
            sync.afterCommit();
        }
        verify(rabbitTemplate).convertAndSend(eq(MessageTypes.EXCHANGE), eq(message.type()), eq(message), any(MessagePostProcessor.class));
    }

    @Test
    void listenerDelegatesToOrchestrator() {
        ServiceOrderSagaOrchestrator orchestrator = mock(ServiceOrderSagaOrchestrator.class);
        new SagaEventListener(orchestrator).onEvent(message);
        verify(orchestrator).handle(message);
    }
}
