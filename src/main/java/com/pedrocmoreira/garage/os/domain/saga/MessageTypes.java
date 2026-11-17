package com.pedrocmoreira.garage.os.domain.saga;

/** Contrato de mensagens (comandos e eventos) compartilhado entre os serviços. */
public final class MessageTypes {
    private MessageTypes() {}

    public static final String EXCHANGE = "garage.saga";
    public static final String DEAD_LETTER_EXCHANGE = "garage.saga.dlx";

    // Comandos enviados pelo orquestrador
    public static final String CMD_EXECUTION_START_DIAGNOSIS = "command.execution.diagnosis.start";
    public static final String CMD_EXECUTION_RESERVE_PARTS = "command.execution.parts.reserve";
    public static final String CMD_EXECUTION_CANCEL = "command.execution.cancel";
    public static final String CMD_BILLING_CREATE_BUDGET = "command.billing.budget.create";
    public static final String CMD_BILLING_CANCEL_BUDGET = "command.billing.budget.cancel";
    public static final String CMD_BILLING_REFUND_PAYMENT = "command.billing.payment.refund";

    // Eventos do Execution Service
    public static final String EVT_DIAGNOSIS_STARTED = "event.execution.diagnosis.started";
    public static final String EVT_DIAGNOSIS_COMPLETED = "event.execution.diagnosis.completed";
    public static final String EVT_PARTS_RESERVED = "event.execution.parts.reserved";
    public static final String EVT_PARTS_RESERVATION_FAILED = "event.execution.parts.reservation-failed";
    public static final String EVT_EXECUTION_COMPLETED = "event.execution.completed";
    public static final String EVT_EXECUTION_CANCELLED = "event.execution.cancelled";

    // Eventos do Billing Service
    public static final String EVT_BUDGET_CREATED = "event.billing.budget.created";
    public static final String EVT_BUDGET_APPROVED = "event.billing.budget.approved";
    public static final String EVT_BUDGET_REJECTED = "event.billing.budget.rejected";
    public static final String EVT_BUDGET_CANCELLED = "event.billing.budget.cancelled";
    public static final String EVT_PAYMENT_CONFIRMED = "event.billing.payment.confirmed";
    public static final String EVT_PAYMENT_FAILED = "event.billing.payment.failed";
    public static final String EVT_PAYMENT_REFUNDED = "event.billing.payment.refunded";

    // Filas
    public static final String QUEUE_OS_EVENTS = "os-service.events";
}
