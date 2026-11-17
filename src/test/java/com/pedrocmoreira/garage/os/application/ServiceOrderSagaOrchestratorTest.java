package com.pedrocmoreira.garage.os.application;

import com.pedrocmoreira.garage.os.TestFixtures;
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
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.pedrocmoreira.garage.os.domain.saga.MessageTypes.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ServiceOrderSagaOrchestratorTest {
    private ServiceOrderRepository serviceOrderRepository;
    private SagaRepository sagaRepository;
    private ProcessedMessageRepository processedMessageRepository;
    private MessagePublisher publisher;
    private StatusChangeService statusChangeService;
    private ServiceOrderSagaOrchestrator orchestrator;
    private ServiceOrder serviceOrder;
    private SagaInstance saga;

    @BeforeEach
    void setUp() {
        serviceOrderRepository = mock(ServiceOrderRepository.class);
        sagaRepository = mock(SagaRepository.class);
        processedMessageRepository = mock(ProcessedMessageRepository.class);
        publisher = mock(MessagePublisher.class);
        statusChangeService = mock(StatusChangeService.class);
        orchestrator = new ServiceOrderSagaOrchestrator(serviceOrderRepository, sagaRepository,
                processedMessageRepository, publisher, statusChangeService, new SimpleMeterRegistry());

        serviceOrder = TestFixtures.serviceOrder(StatusSO.RECEBIDA);
        saga = SagaInstance.builder().serviceOrderId(serviceOrder.getId()).build();

        when(processedMessageRepository.registerIfNew(any())).thenReturn(true);
        when(serviceOrderRepository.findById(serviceOrder.getId())).thenReturn(Optional.of(serviceOrder));
        when(serviceOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(sagaRepository.findByServiceOrderId(serviceOrder.getId())).thenReturn(Optional.of(saga));
        when(sagaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(statusChangeService.change(any(), any(), any())).thenAnswer(inv -> {
            ServiceOrder so = inv.getArgument(0);
            so.changeStatus(inv.getArgument(1));
            return so;
        });
    }

    private SagaMessage event(String type, Map<String, Object> data) {
        return SagaMessage.of(type, serviceOrder.getId(), serviceOrder.getNumber(), data);
    }

    private List<SagaMessage> published() {
        ArgumentCaptor<SagaMessage> captor = ArgumentCaptor.forClass(SagaMessage.class);
        verify(publisher, atLeastOnce()).publish(captor.capture());
        return captor.getAllValues();
    }

    @Test
    void startShouldCreateSagaAndRequestDiagnosis() {
        SagaInstance created = orchestrator.start(serviceOrder);

        assertThat(created.getCurrentStep()).isEqualTo(SagaStep.DIAGNOSIS_REQUESTED);
        SagaMessage cmd = published().get(0);
        assertThat(cmd.type()).isEqualTo(CMD_EXECUTION_START_DIAGNOSIS);
        assertThat(cmd.data()).containsKeys("services", "parts", "vehiclePlate");
    }

    @Test
    void diagnosisStartedShouldMoveOrderToDiagnosis() {
        orchestrator.handle(event(EVT_DIAGNOSIS_STARTED, Map.of()));
        assertThat(serviceOrder.getStatus()).isEqualTo(StatusSO.EM_DIAGNOSTICO);
        assertThat(saga.getCurrentStep()).isEqualTo(SagaStep.DIAGNOSIS_IN_PROGRESS);
    }

    @Test
    void diagnosisCompletedShouldRequestBudget() {
        orchestrator.handle(event(EVT_DIAGNOSIS_COMPLETED, Map.of("notes", "Trocar óleo")));

        assertThat(serviceOrder.getStatus()).isEqualTo(StatusSO.EM_DIAGNOSTICO);
        assertThat(serviceOrder.getDiagnosisNotes()).isEqualTo("Trocar óleo");
        assertThat(saga.getCurrentStep()).isEqualTo(SagaStep.BUDGET_REQUESTED);
        SagaMessage cmd = published().get(0);
        assertThat(cmd.type()).isEqualTo(CMD_BILLING_CREATE_BUDGET);
        assertThat(cmd.data()).containsEntry("customerEmail", "joao@email.com").containsKey("totalAmount");
    }

    @Test
    void budgetCreatedShouldWaitApproval() {
        serviceOrder.setStatus(StatusSO.EM_DIAGNOSTICO);
        orchestrator.handle(event(EVT_BUDGET_CREATED, Map.of()));
        assertThat(saga.isBudgetCreated()).isTrue();
        assertThat(saga.getCurrentStep()).isEqualTo(SagaStep.AWAITING_APPROVAL);
        assertThat(serviceOrder.getStatus()).isEqualTo(StatusSO.AGUARDANDO_APROVACAO);
    }

    @Test
    void budgetApprovedShouldWaitPayment() {
        serviceOrder.setStatus(StatusSO.AGUARDANDO_APROVACAO);
        orchestrator.handle(event(EVT_BUDGET_APPROVED, Map.of("paymentUrl", "http://pay")));
        assertThat(serviceOrder.getPaymentUrl()).isEqualTo("http://pay");
        assertThat(serviceOrder.getStatus()).isEqualTo(StatusSO.AGUARDANDO_PAGAMENTO);
        assertThat(saga.getCurrentStep()).isEqualTo(SagaStep.AWAITING_PAYMENT);
    }

    @Test
    void paymentConfirmedShouldRequestPartsReservation() {
        serviceOrder.setStatus(StatusSO.AGUARDANDO_PAGAMENTO);
        orchestrator.handle(event(EVT_PAYMENT_CONFIRMED, Map.of()));
        assertThat(saga.isPaymentConfirmed()).isTrue();
        assertThat(published().get(0).type()).isEqualTo(CMD_EXECUTION_RESERVE_PARTS);
    }

    @Test
    void partsReservedShouldStartExecution() {
        serviceOrder.setStatus(StatusSO.AGUARDANDO_PAGAMENTO);
        orchestrator.handle(event(EVT_PARTS_RESERVED, Map.of()));
        assertThat(saga.isPartsReserved()).isTrue();
        assertThat(serviceOrder.getStatus()).isEqualTo(StatusSO.EM_EXECUCAO);
    }

    @Test
    void executionCompletedShouldFinishSaga() {
        serviceOrder.setStatus(StatusSO.EM_EXECUCAO);
        orchestrator.handle(event(EVT_EXECUTION_COMPLETED, Map.of()));
        assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPLETED);
        assertThat(serviceOrder.getStatus()).isEqualTo(StatusSO.FINALIZADA);
    }

    @Test
    void budgetRejectedShouldCancelExecutionAndBudget() {
        serviceOrder.setStatus(StatusSO.AGUARDANDO_APROVACAO);
        saga.setBudgetCreated(true);

        orchestrator.handle(event(EVT_BUDGET_REJECTED, Map.of()));

        assertThat(published()).extracting(SagaMessage::type)
                .containsExactly(CMD_EXECUTION_CANCEL, CMD_BILLING_CANCEL_BUDGET);
        assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPENSATED);
        assertThat(serviceOrder.getStatus()).isEqualTo(StatusSO.CANCELADA);
        assertThat(serviceOrder.getCancellationReason()).contains("recusado");
    }

    @Test
    void paymentFailedShouldCompensate() {
        serviceOrder.setStatus(StatusSO.AGUARDANDO_PAGAMENTO);
        saga.setBudgetCreated(true);
        orchestrator.handle(event(EVT_PAYMENT_FAILED, Map.of("reason", "cartão recusado")));
        assertThat(saga.getFailureReason()).contains("cartão recusado");
        assertThat(serviceOrder.getStatus()).isEqualTo(StatusSO.CANCELADA);
    }

    @Test
    void partsReservationFailureAfterPaymentShouldRefund() {
        serviceOrder.setStatus(StatusSO.AGUARDANDO_PAGAMENTO);
        saga.setBudgetCreated(true);
        saga.setPaymentConfirmed(true);

        orchestrator.handle(event(EVT_PARTS_RESERVATION_FAILED, Map.of("reason", "sem estoque")));

        assertThat(published()).extracting(SagaMessage::type)
                .containsExactly(CMD_EXECUTION_CANCEL, CMD_BILLING_REFUND_PAYMENT);
        assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPENSATING);

        orchestrator.handle(event(EVT_PAYMENT_REFUNDED, Map.of()));
        assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPENSATED);
    }

    @Test
    void refundEventOnNonCompensatingSagaIsIgnored() {
        orchestrator.handle(event(EVT_PAYMENT_REFUNDED, Map.of()));
        assertThat(saga.getStatus()).isEqualTo(SagaStatus.STARTED);
    }

    @Test
    void duplicatedMessageShouldBeIgnored() {
        when(processedMessageRepository.registerIfNew(any())).thenReturn(false);
        orchestrator.handle(event(EVT_DIAGNOSIS_STARTED, Map.of()));
        verifyNoInteractions(serviceOrderRepository, statusChangeService);
    }

    @Test
    void eventsAfterSagaFinishedAreIgnored() {
        saga.setStatus(SagaStatus.COMPENSATED);
        orchestrator.handle(event(EVT_PARTS_RESERVED, Map.of()));
        verifyNoInteractions(statusChangeService);
    }

    @Test
    void unknownEventIsIgnored() {
        orchestrator.handle(event(EVT_EXECUTION_CANCELLED, Map.of()));
        verifyNoInteractions(publisher, statusChangeService);
    }

    @Test
    void eventForUnknownOrderShouldFail() {
        SagaMessage msg = SagaMessage.of(EVT_DIAGNOSIS_STARTED, 999L, "X", Map.of());
        assertThatThrownBy(() -> orchestrator.handle(msg)).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void manualCancelShouldCompensate() {
        orchestrator.cancel(serviceOrder.getId(), "Cliente desistiu");
        assertThat(serviceOrder.getStatus()).isEqualTo(StatusSO.CANCELADA);
        assertThat(published()).extracting(SagaMessage::type).containsExactly(CMD_EXECUTION_CANCEL);
    }

    @Test
    void manualCancelWithoutReasonUsesDefault() {
        orchestrator.cancel(serviceOrder.getId(), null);
        assertThat(serviceOrder.getCancellationReason()).contains("sem motivo informado");
    }

    @Test
    void manualCancelOfFinishedOrderShouldFail() {
        serviceOrder.setStatus(StatusSO.FINALIZADA);
        assertThatThrownBy(() -> orchestrator.cancel(serviceOrder.getId(), "x"))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }
}
