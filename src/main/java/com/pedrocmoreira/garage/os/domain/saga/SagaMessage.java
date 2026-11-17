package com.pedrocmoreira.garage.os.domain.saga;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Envelope padrão trafegado no RabbitMQ entre os microsserviços.
 * O campo {@code type} também é usado como routing key na exchange {@code garage.saga}.
 */
public record SagaMessage(
        String messageId,
        String type,
        Long serviceOrderId,
        String serviceOrderNumber,
        Instant occurredAt,
        Map<String, Object> data
) {
    public static SagaMessage of(String type, Long serviceOrderId, String serviceOrderNumber, Map<String, Object> data) {
        return new SagaMessage(UUID.randomUUID().toString(), type, serviceOrderId, serviceOrderNumber,
                Instant.now(), data == null ? Map.of() : new HashMap<>(data));
    }

    public String dataAsString(String key) {
        Object value = data == null ? null : data.get(key);
        return value == null ? null : value.toString();
    }
}
