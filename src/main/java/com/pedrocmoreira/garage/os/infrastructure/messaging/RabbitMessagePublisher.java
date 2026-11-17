package com.pedrocmoreira.garage.os.infrastructure.messaging;

import com.pedrocmoreira.garage.os.domain.port.MessagePublisher;
import com.pedrocmoreira.garage.os.domain.saga.MessageTypes;
import com.pedrocmoreira.garage.os.domain.saga.SagaMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Publica mensagens somente após o commit da transação local, evitando enviar comandos
 * referentes a um estado que sofreu rollback.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMessagePublisher implements MessagePublisher {
    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(SagaMessage message) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(message);
                }
            });
        } else {
            send(message);
        }
    }

    void send(SagaMessage message) {
        log.info("Publicando {} (OS {}, msg {})", message.type(), message.serviceOrderNumber(), message.messageId());
        rabbitTemplate.convertAndSend(MessageTypes.EXCHANGE, message.type(), message, m -> {
            m.getMessageProperties().setMessageId(message.messageId());
            return m;
        });
    }
}
