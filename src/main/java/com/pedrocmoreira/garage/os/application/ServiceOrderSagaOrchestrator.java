package com.pedrocmoreira.garage.os.application;

import com.pedrocmoreira.garage.os.domain.exception.EntityNotFoundException;
import com.pedrocmoreira.garage.os.domain.exception.InvalidStatusTransitionException;
import com.pedrocmoreira.garage.os.domain.model.ServiceOrder;
import com.pedrocmoreira.garage.os.domain.model.StatusSO;
import com.pedrocmoreira.garage.os.domain.port.MessagePublisher;
import com.pedrocmoreira.garage.os.domain.repository.ProcessedMessageRepository;
import com.pedrocmoreira.garage.os.domain.repository.SagaRepository;
import com.pedrocmoreira.garage.os.domain.repository.ServiceOrderRepository;
import com.pedrocmoreira.garage.os.domain.saga.SagaInstance;
import com.pedrocmoreira.garage.os.domain.saga.SagaMessage;
import com.pedrocmoreira.garage.os.domain.saga.SagaStatus;
import com.pedrocmoreira.garage.os.domain.saga.SagaStep;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.pedrocmoreira.garage.os.domain.saga.MessageTypes.*;

/**
 * Orquestrador central da Saga da Ordem de Serviço.
 *
 * <pre>
 * Abrir OS -> diagnóstico (Execution) -> orçamento (Billing) -> aprovação do cliente (Billing)
 *          -> pagamento Mercado Pago (Billing) -> reserva de peças + execução (Execution) -> finalizada
 * </pre>
 *
 * Em qualquer falha (orçamento recusado, pagamento recusado, falta de peças, cancelamento manual)
 * o orquestrador dispara as compensações das etapas já efetivadas, em ordem inversa.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceOrderSagaOrchestrator {
    private final ServiceOrderRepository serviceOrderRepository;
    private final SagaRepository sagaRepository;
    private final ProcessedMessageRepository processedMessageRepository;
    private final MessagePublisher publisher;
    private final StatusChangeService statusChangeService;
    private final MeterRegistry meterRegistry;

    /** Primeira etapa: cria a saga e solicita o diagnóstico ao Execution Service. */
    @Transactional
    public SagaInstance start(ServiceOrder serviceOrder) {
        SagaInstance saga = sagaRepository.save(SagaInstance.builder()
                .serviceOrderId(serviceOrder.getId())
                .currentStep(SagaStep.DIAGNOSIS_REQUESTED)
                .build());

        Map<String, Object> data = new HashMap<>();
        data.put("vehiclePlate", serviceOrder.getVehicle().getPlate());
        data.put("vehicleDescription", serviceOrder.getVehicle().description());
        data.put("observations", serviceOrder.getObservations());
        data.put("services", serviceItems(serviceOrder));
        data.put("parts", partItems(serviceOrder));

        publisher.publish(SagaMessage.of(CMD_EXECUTION_START_DIAGNOSIS, serviceOrder.getId(), serviceOrder.getNumber(), data));
        meterRegistry.counter("garage.saga.started").increment();
        log.info("Saga {} iniciada para OS {}", saga.getId(), serviceOrder.getNumber());
        return saga;
    }

    /** Trata os eventos publicados pelos serviços participantes. */
    @Transactional
    public void handle(SagaMessage message) {
        if (!processedMessageRepository.registerIfNew(message.messageId())) {
            log.info("Mensagem {} ({}) já processada - ignorando", message.messageId(), message.type());
            return;
        }

        ServiceOrder serviceOrder = serviceOrderRepository.findById(message.serviceOrderId())
                .orElseThrow(() -> new EntityNotFoundException("Ordem de serviço", message.serviceOrderId()));
        SagaInstance saga = sagaRepository.findByServiceOrderId(serviceOrder.getId())
                .orElseThrow(() -> new EntityNotFoundException("Saga da OS", serviceOrder.getId()));

        log.info("Saga {} recebeu {} (etapa atual {}, status {})", saga.getId(), message.type(), saga.getCurrentStep(), saga.getStatus());

        if (EVT_PAYMENT_REFUNDED.equals(message.type())) {
            onPaymentRefunded(saga);
            return;
        }
        if (!saga.isActive()) {
            log.warn("Saga {} não está ativa ({}). Evento {} ignorado.", saga.getId(), saga.getStatus(), message.type());
            return;
        }

        switch (message.type()) {
            case EVT_DIAGNOSIS_STARTED -> onDiagnosisStarted(saga, serviceOrder);
            case EVT_DIAGNOSIS_COMPLETED -> onDiagnosisCompleted(saga, serviceOrder, message);
            case EVT_BUDGET_CREATED -> onBudgetCreated(saga, serviceOrder);
            case EVT_BUDGET_APPROVED -> onBudgetApproved(saga, serviceOrder, message);
            case EVT_BUDGET_REJECTED -> compensate(saga, serviceOrder, "Orçamento recusado pelo cliente");
            case EVT_PAYMENT_CONFIRMED -> onPaymentConfirmed(saga, serviceOrder);
            case EVT_PAYMENT_FAILED -> compensate(saga, serviceOrder,
                    "Pagamento não aprovado: " + defaultIfNull(message.dataAsString("reason"), "recusado"));
            case EVT_PARTS_RESERVED -> onPartsReserved(saga, serviceOrder);
            case EVT_PARTS_RESERVATION_FAILED -> compensate(saga, serviceOrder,
                    "Falha na reserva de peças: " + defaultIfNull(message.dataAsString("reason"), "estoque insuficiente"));
            case EVT_EXECUTION_COMPLETED -> onExecutionCompleted(saga, serviceOrder);
            default -> log.debug("Evento {} sem ação no orquestrador", message.type());
        }
    }

    /** Cancelamento solicitado pela oficina/cliente: aciona a compensação da saga. */
    @Transactional
    public ServiceOrder cancel(Long serviceOrderId, String reason) {
        ServiceOrder serviceOrder = serviceOrderRepository.findById(serviceOrderId)
                .orElseThrow(() -> new EntityNotFoundException("Ordem de serviço", serviceOrderId));
        if (!serviceOrder.getStatus().canTransitionTo(StatusSO.CANCELADA)) {
            throw new InvalidStatusTransitionException("A OS não pode ser cancelada no status " + serviceOrder.getStatus());
        }
        SagaInstance saga = sagaRepository.findByServiceOrderId(serviceOrderId)
                .orElseThrow(() -> new EntityNotFoundException("Saga da OS", serviceOrderId));
        compensate(saga, serviceOrder, "Cancelada manualmente: " + defaultIfNull(reason, "sem motivo informado"));
        return serviceOrder;
    }

    private void onDiagnosisStarted(SagaInstance saga, ServiceOrder serviceOrder) {
        ensureInDiagnosis(serviceOrder, "Diagnóstico iniciado pelo mecânico");
        saga.moveTo(SagaStep.DIAGNOSIS_IN_PROGRESS);
        sagaRepository.save(saga);
    }

    private void onDiagnosisCompleted(SagaInstance saga, ServiceOrder serviceOrder, SagaMessage message) {
        ensureInDiagnosis(serviceOrder, "Diagnóstico iniciado pelo mecânico");
        serviceOrder.setDiagnosisNotes(message.dataAsString("notes"));
        serviceOrderRepository.save(serviceOrder);

        saga.moveTo(SagaStep.BUDGET_REQUESTED);
        sagaRepository.save(saga);

        Map<String, Object> data = new HashMap<>();
        data.put("customerName", serviceOrder.getCustomer().getName());
        data.put("customerEmail", serviceOrder.getCustomer().getEmail());
        data.put("customerDocument", serviceOrder.getCustomer().getDocument());
        data.put("vehicleDescription", serviceOrder.getVehicle().description());
        data.put("diagnosisNotes", serviceOrder.getDiagnosisNotes());
        data.put("totalAmount", serviceOrder.getTotalValue());
        data.put("services", serviceItems(serviceOrder));
        data.put("parts", partItems(serviceOrder));
        publisher.publish(SagaMessage.of(CMD_BILLING_CREATE_BUDGET, serviceOrder.getId(), serviceOrder.getNumber(), data));
    }

    private void onBudgetCreated(SagaInstance saga, ServiceOrder serviceOrder) {
        saga.setBudgetCreated(true);
        saga.moveTo(SagaStep.AWAITING_APPROVAL);
        sagaRepository.save(saga);
        statusChangeService.change(serviceOrder, StatusSO.AGUARDANDO_APROVACAO, "Orçamento gerado e enviado ao cliente");
    }

    private void onBudgetApproved(SagaInstance saga, ServiceOrder serviceOrder, SagaMessage message) {
        serviceOrder.setPaymentUrl(message.dataAsString("paymentUrl"));
        saga.moveTo(SagaStep.AWAITING_PAYMENT);
        sagaRepository.save(saga);
        statusChangeService.change(serviceOrder, StatusSO.AGUARDANDO_PAGAMENTO, "Orçamento aprovado pelo cliente");
    }

    private void onPaymentConfirmed(SagaInstance saga, ServiceOrder serviceOrder) {
        saga.setPaymentConfirmed(true);
        saga.moveTo(SagaStep.PARTS_RESERVATION_REQUESTED);
        sagaRepository.save(saga);
        publisher.publish(SagaMessage.of(CMD_EXECUTION_RESERVE_PARTS, serviceOrder.getId(), serviceOrder.getNumber(),
                Map.of("parts", partItems(serviceOrder))));
    }

    private void onPartsReserved(SagaInstance saga, ServiceOrder serviceOrder) {
        saga.setPartsReserved(true);
        saga.moveTo(SagaStep.EXECUTING);
        sagaRepository.save(saga);
        statusChangeService.change(serviceOrder, StatusSO.EM_EXECUCAO, "Pagamento confirmado e peças reservadas");
    }

    private void onExecutionCompleted(SagaInstance saga, ServiceOrder serviceOrder) {
        saga.moveTo(SagaStep.DONE);
        saga.setStatus(SagaStatus.COMPLETED);
        sagaRepository.save(saga);
        statusChangeService.change(serviceOrder, StatusSO.FINALIZADA, "Execução concluída pela produção");
        meterRegistry.counter("garage.saga.completed").increment();
    }

    private void onPaymentRefunded(SagaInstance saga) {
        if (saga.getStatus() == SagaStatus.COMPENSATING) {
            saga.setStatus(SagaStatus.COMPENSATED);
            saga.moveTo(SagaStep.COMPENSATION);
            sagaRepository.save(saga);
            log.info("Saga {} compensada (estorno confirmado)", saga.getId());
        }
    }

    /**
     * Compensação: desfaz os efeitos já aplicados nos outros serviços (ordem inversa) e cancela a OS.
     */
    void compensate(SagaInstance saga, ServiceOrder serviceOrder, String reason) {
        log.warn("Saga {} entrando em compensação: {}", saga.getId(), reason);
        Long id = serviceOrder.getId();
        String number = serviceOrder.getNumber();

        // 1) Execução: libera peças reservadas / remove da fila
        publisher.publish(SagaMessage.of(CMD_EXECUTION_CANCEL, id, number, Map.of("reason", reason)));

        // 2) Billing: estorna o pagamento (se houve) ou cancela o orçamento
        boolean waitRefund = false;
        if (saga.isPaymentConfirmed()) {
            publisher.publish(SagaMessage.of(CMD_BILLING_REFUND_PAYMENT, id, number, Map.of("reason", reason)));
            waitRefund = true;
        } else if (saga.isBudgetCreated()) {
            publisher.publish(SagaMessage.of(CMD_BILLING_CANCEL_BUDGET, id, number, Map.of("reason", reason)));
        }

        saga.setFailureReason(reason);
        saga.setStatus(waitRefund ? SagaStatus.COMPENSATING : SagaStatus.COMPENSATED);
        saga.moveTo(SagaStep.COMPENSATION);
        sagaRepository.save(saga);

        serviceOrder.setCancellationReason(reason);
        if (serviceOrder.getStatus().canTransitionTo(StatusSO.CANCELADA)) {
            statusChangeService.change(serviceOrder, StatusSO.CANCELADA, reason);
        }
        meterRegistry.counter("garage.saga.compensated").increment();
    }

    private void ensureInDiagnosis(ServiceOrder serviceOrder, String note) {
        if (serviceOrder.getStatus() == StatusSO.RECEBIDA) {
            statusChangeService.change(serviceOrder, StatusSO.EM_DIAGNOSTICO, note);
        }
    }

    private static List<Map<String, Object>> serviceItems(ServiceOrder serviceOrder) {
        return serviceOrder.getServiceItems().stream()
                .map(i -> Map.<String, Object>of(
                        "serviceId", i.getService().getId(),
                        "name", i.getService().getName(),
                        "quantity", 1,
                        "unitPrice", i.getAppliedPrice()))
                .toList();
    }

    private static List<Map<String, Object>> partItems(ServiceOrder serviceOrder) {
        return serviceOrder.getPartItems().stream()
                .map(i -> Map.<String, Object>of(
                        "partId", i.getPart().getId(),
                        "name", i.getPart().getName(),
                        "quantity", i.getQuantity(),
                        "unitPrice", i.getUnitPriceApplied() == null ? BigDecimal.ZERO : i.getUnitPriceApplied()))
                .toList();
    }

    private static String defaultIfNull(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
