package com.pedrocmoreira.garage.os.domain;

import com.pedrocmoreira.garage.os.TestFixtures;
import com.pedrocmoreira.garage.os.domain.exception.InvalidStatusTransitionException;
import com.pedrocmoreira.garage.os.domain.model.ServiceOrder;
import com.pedrocmoreira.garage.os.domain.model.StatusSO;
import com.pedrocmoreira.garage.os.domain.saga.SagaInstance;
import com.pedrocmoreira.garage.os.domain.saga.SagaMessage;
import com.pedrocmoreira.garage.os.domain.saga.SagaStatus;
import com.pedrocmoreira.garage.os.domain.saga.SagaStep;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class ServiceOrderDomainTest {

    @Test
    void shouldCalculateTotalWithServicesAndParts() {
        ServiceOrder so = TestFixtures.serviceOrder(StatusSO.RECEBIDA);
        assertThat(so.getTotalValue()).isEqualByComparingTo("302.00");
    }

    @Test
    void shouldFollowHappyPathTransitionsAndRegisterTimestamps() {
        ServiceOrder so = TestFixtures.serviceOrder(StatusSO.RECEBIDA);
        assertThat(so.changeStatus(StatusSO.EM_DIAGNOSTICO)).isEqualTo(StatusSO.RECEBIDA);
        assertThat(so.getDiagnosisStartDate()).isNotNull();
        so.changeStatus(StatusSO.AGUARDANDO_APROVACAO);
        so.changeStatus(StatusSO.AGUARDANDO_PAGAMENTO);
        so.changeStatus(StatusSO.EM_EXECUCAO);
        assertThat(so.getExecutionStartDate()).isNotNull();
        assertThat(so.getExecutionTimeInMinutes()).isNull();
        so.changeStatus(StatusSO.FINALIZADA);
        assertThat(so.getCompletionDate()).isNotNull();
        assertThat(so.getExecutionTimeInMinutes()).isZero();
        so.changeStatus(StatusSO.ENTREGUE);
        assertThat(so.getDeliveryDate()).isNotNull();
        assertThat(so.getStatus().isFinal()).isTrue();
    }

    @Test
    void shouldRejectInvalidTransition() {
        ServiceOrder so = TestFixtures.serviceOrder(StatusSO.RECEBIDA);
        assertThatThrownBy(() -> so.changeStatus(StatusSO.FINALIZADA))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("RECEBIDA -> FINALIZADA");
    }

    @ParameterizedTest
    @EnumSource(value = StatusSO.class, names = {"RECEBIDA", "EM_DIAGNOSTICO", "AGUARDANDO_APROVACAO", "AGUARDANDO_PAGAMENTO", "EM_EXECUCAO"})
    void shouldAllowCancellationFromActiveStatus(StatusSO status) {
        assertThat(status.canTransitionTo(StatusSO.CANCELADA)).isTrue();
        assertThat(status.isFinal()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = StatusSO.class, names = {"FINALIZADA", "ENTREGUE", "CANCELADA"})
    void shouldNotAllowCancellationAfterCompletion(StatusSO status) {
        assertThat(status.canTransitionTo(StatusSO.CANCELADA)).isFalse();
    }

    @Test
    void sagaInstanceShouldTrackStep() {
        SagaInstance saga = SagaInstance.builder().serviceOrderId(1L).build();
        assertThat(saga.getId()).isNotBlank();
        assertThat(saga.isActive()).isTrue();
        saga.moveTo(SagaStep.EXECUTING);
        assertThat(saga.getCurrentStep()).isEqualTo(SagaStep.EXECUTING);
        saga.setStatus(SagaStatus.COMPLETED);
        assertThat(saga.isActive()).isFalse();
    }

    @Test
    void sagaMessageShouldExposeData() {
        SagaMessage msg = SagaMessage.of("x", 1L, "OS-1", Map.of("reason", "falha", "amount", new BigDecimal("10")));
        assertThat(msg.messageId()).isNotBlank();
        assertThat(msg.dataAsString("reason")).isEqualTo("falha");
        assertThat(msg.dataAsString("amount")).isEqualTo("10");
        assertThat(msg.dataAsString("missing")).isNull();
        assertThat(SagaMessage.of("x", 1L, "OS-1", null).data()).isEmpty();
        assertThat(new SagaMessage("id", "t", 1L, "n", null, null).dataAsString("a")).isNull();
    }

    @Test
    void vehicleDescriptionShouldIncludePlate() {
        assertThat(TestFixtures.vehicle(TestFixtures.customer()).description()).isEqualTo("Fiat Uno 2020 (ABC1D23)");
    }
}
