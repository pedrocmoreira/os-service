package com.pedrocmoreira.garage.os.domain.saga;

/** Etapas do fluxo transacional da OS coordenadas pelo orquestrador. */
public enum SagaStep {
    DIAGNOSIS_REQUESTED,
    DIAGNOSIS_IN_PROGRESS,
    BUDGET_REQUESTED,
    AWAITING_APPROVAL,
    AWAITING_PAYMENT,
    PARTS_RESERVATION_REQUESTED,
    EXECUTING,
    DONE,
    COMPENSATION
}
