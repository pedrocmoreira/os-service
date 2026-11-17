package com.pedrocmoreira.garage.os.domain.repository;

/** Garante idempotência no consumo de mensagens (entrega "at-least-once" do RabbitMQ). */
public interface ProcessedMessageRepository {
    /** @return true se a mensagem ainda não havia sido processada (e agora foi registrada). */
    boolean registerIfNew(String messageId);
}
