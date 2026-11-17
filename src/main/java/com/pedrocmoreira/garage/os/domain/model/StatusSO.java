package com.pedrocmoreira.garage.os.domain.model;

import com.pedrocmoreira.garage.os.domain.exception.InvalidStatusTransitionException;

import java.util.Map;
import java.util.Set;

/**
 * Máquina de estados da Ordem de Serviço.
 * As transições são disparadas pelo orquestrador da Saga a partir dos eventos dos demais serviços.
 */
public enum StatusSO {
    RECEBIDA,
    EM_DIAGNOSTICO,
    AGUARDANDO_APROVACAO,
    AGUARDANDO_PAGAMENTO,
    EM_EXECUCAO,
    FINALIZADA,
    ENTREGUE,
    CANCELADA;

    private static final Map<StatusSO, Set<StatusSO>> ALLOWED_TRANSITIONS = Map.of(
            RECEBIDA,             Set.of(EM_DIAGNOSTICO, CANCELADA),
            EM_DIAGNOSTICO,       Set.of(AGUARDANDO_APROVACAO, CANCELADA),
            AGUARDANDO_APROVACAO, Set.of(AGUARDANDO_PAGAMENTO, CANCELADA),
            AGUARDANDO_PAGAMENTO, Set.of(EM_EXECUCAO, CANCELADA),
            EM_EXECUCAO,          Set.of(FINALIZADA, CANCELADA),
            FINALIZADA,           Set.of(ENTREGUE),
            ENTREGUE,             Set.of(),
            CANCELADA,            Set.of()
    );

    public StatusSO transitionTo(StatusSO next) {
        if (!canTransitionTo(next)) {
            throw new InvalidStatusTransitionException(String.format(
                    "Transição inválida %s -> %s. Transições permitidas: %s",
                    this, next, ALLOWED_TRANSITIONS.get(this)));
        }
        return next;
    }

    public boolean canTransitionTo(StatusSO next) {
        return ALLOWED_TRANSITIONS.getOrDefault(this, Set.of()).contains(next);
    }

    public boolean isFinal() {
        return this == ENTREGUE || this == CANCELADA;
    }
}
