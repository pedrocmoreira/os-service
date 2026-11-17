package com.pedrocmoreira.garage.os.domain.saga;

public enum SagaStatus {
    /** Saga em andamento. */
    STARTED,
    /** Uma etapa falhou e as compensações foram disparadas (aguardando confirmação). */
    COMPENSATING,
    /** Todas as compensações foram concluídas. */
    COMPENSATED,
    /** Fluxo concluído com sucesso. */
    COMPLETED
}
