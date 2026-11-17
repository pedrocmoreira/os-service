package com.pedrocmoreira.garage.os.infrastructure.messaging;

import com.pedrocmoreira.garage.os.application.ServiceOrderSagaOrchestrator;
import com.pedrocmoreira.garage.os.domain.saga.MessageTypes;
import com.pedrocmoreira.garage.os.domain.saga.SagaMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SagaEventListener {
    private final ServiceOrderSagaOrchestrator orchestrator;

    @RabbitListener(queues = MessageTypes.QUEUE_OS_EVENTS)
    public void onEvent(SagaMessage message) {
        orchestrator.handle(message);
    }
}
