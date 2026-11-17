package com.pedrocmoreira.garage.os.domain.port;

import com.pedrocmoreira.garage.os.domain.saga.SagaMessage;

/** Porta de saída para publicação de comandos/eventos na mensageria. */
public interface MessagePublisher {
    void publish(SagaMessage message);
}
