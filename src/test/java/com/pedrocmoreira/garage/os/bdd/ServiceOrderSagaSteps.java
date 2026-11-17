package com.pedrocmoreira.garage.os.bdd;

import com.pedrocmoreira.garage.os.application.CreateServiceOrderUseCase;
import com.pedrocmoreira.garage.os.application.ServiceOrderSagaOrchestrator;
import com.pedrocmoreira.garage.os.application.StatusChangeService;
import com.pedrocmoreira.garage.os.domain.model.*;
import com.pedrocmoreira.garage.os.domain.saga.SagaMessage;
import com.pedrocmoreira.garage.os.infrastructure.persistence.CatalogPartJpaRepository;
import com.pedrocmoreira.garage.os.infrastructure.persistence.CatalogServiceJpaRepository;
import com.pedrocmoreira.garage.os.infrastructure.persistence.CustomerJpaRepository;
import com.pedrocmoreira.garage.os.infrastructure.persistence.VehicleJpaRepository;
import io.cucumber.java.Before;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.E;
import io.cucumber.java.pt.Então;
import io.cucumber.java.pt.Quando;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServiceOrderSagaSteps {
    private InMemoryRepositories.Orders orders;
    private InMemoryRepositories.Sagas sagas;
    private InMemoryRepositories.History history;
    private InMemoryRepositories.RecordingPublisher publisher;
    private CustomerJpaRepository customers;
    private VehicleJpaRepository vehicles;
    private CatalogServiceJpaRepository catalogServices;
    private CatalogPartJpaRepository catalogParts;
    private CreateServiceOrderUseCase createUseCase;
    private ServiceOrderSagaOrchestrator orchestrator;
    private StatusChangeService statusChangeService;
    private ServiceOrder current;

    @Before
    public void setUp() {
        orders = new InMemoryRepositories.Orders();
        sagas = new InMemoryRepositories.Sagas();
        history = new InMemoryRepositories.History();
        publisher = new InMemoryRepositories.RecordingPublisher();
        customers = mock(CustomerJpaRepository.class);
        vehicles = mock(VehicleJpaRepository.class);
        catalogServices = mock(CatalogServiceJpaRepository.class);
        catalogParts = mock(CatalogPartJpaRepository.class);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        statusChangeService = new StatusChangeService(orders, history, registry);
        orchestrator = new ServiceOrderSagaOrchestrator(orders, sagas, new InMemoryRepositories.Processed(),
                publisher, statusChangeService, registry);
        createUseCase = new CreateServiceOrderUseCase(orders, customers, vehicles, catalogServices, catalogParts,
                statusChangeService, orchestrator);
    }

    @Dado("que existe o cliente {string} com o veículo de placa {string}")
    public void clienteComVeiculo(String name, String plate) {
        Customer customer = Customer.builder().id(1L).name(name).document("52998224725")
                .documentType(Customer.DocumentType.CPF).email("cliente@email.com").build();
        Vehicle vehicle = Vehicle.builder().id(1L).plate(plate).make("Fiat").model("Uno").year(2020).customer(customer).build();
        when(customers.findById(1L)).thenReturn(Optional.of(customer));
        when(vehicles.findById(1L)).thenReturn(Optional.of(vehicle));
    }

    @E("o catálogo possui o serviço {string} por {double} e a peça {string} por {double}")
    public void catalogo(String service, double servicePrice, String part, double partPrice) {
        when(catalogServices.findById(1L)).thenReturn(Optional.of(
                CatalogService.builder().id(1L).name(service).basePrice(BigDecimal.valueOf(servicePrice)).build()));
        when(catalogParts.findById(1L)).thenReturn(Optional.of(
                CatalogPart.builder().id(1L).name(part).unitPrice(BigDecimal.valueOf(partPrice)).build()));
    }

    @Quando("a atendente abre uma OS com o serviço {string} e {int} unidades da peça {string}")
    public void abreOs(String service, int quantity, String part) {
        current = createUseCase.execute(new CreateServiceOrderUseCase.Input(1L, 1L, List.of(1L),
                List.of(new CreateServiceOrderUseCase.PartItemInput(1L, quantity)), "Cliente relata barulho"));
    }

    @Dado("que uma OS foi aberta e o orçamento já foi enviado ao cliente")
    public void osComOrcamentoEnviado() {
        abreOs("Troca de óleo", 4, "Óleo 5W30");
        evento("event.execution.diagnosis.started");
        evento("event.execution.diagnosis.completed");
        evento("event.billing.budget.created");
        publisher.messages.clear();
    }

    @Quando("o evento {string} for recebido")
    public void evento(String type) {
        orchestrator.handle(message(type, Map.of("paymentUrl", "https://mercadopago/checkout", "notes", "Óleo vencido")));
    }

    @Quando("o evento {string} for recebido com motivo {string}")
    public void eventoComMotivo(String type, String reason) {
        orchestrator.handle(message(type, Map.of("reason", reason)));
    }

    @Quando("o mesmo evento {string} for recebido duas vezes")
    public void eventoDuplicado(String type) {
        SagaMessage message = message(type, Map.of("paymentUrl", "https://mercadopago/checkout"));
        orchestrator.handle(message);
        orchestrator.handle(message);
    }

    @Quando("a atendente registra a entrega do veículo")
    public void entrega() {
        statusChangeService.change(reload(), StatusSO.ENTREGUE, "Veículo entregue");
    }

    @Então("a OS deve estar com status {string} e valor total {double}")
    public void statusEValor(String status, double total) {
        statusDaOs(status);
        assertThat(reload().getTotalValue()).isEqualByComparingTo(BigDecimal.valueOf(total));
    }

    @Então("a OS deve estar com status {string}")
    public void statusDaOs(String status) {
        assertThat(reload().getStatus()).isEqualTo(StatusSO.valueOf(status));
    }

    @Então("o comando {string} deve ter sido publicado")
    public void comandoPublicado(String type) {
        assertThat(publisher.types()).contains(type);
    }

    @Então("os comandos de compensação {string} devem ter sido publicados")
    public void comandosCompensacao(String types) {
        List<String> expected = Arrays.asList(types.split(","));
        List<String> published = publisher.types();
        assertThat(published.subList(published.size() - expected.size(), published.size()))
                .as("últimos comandos publicados (compensação)")
                .containsExactlyElementsOf(expected);
    }

    @Então("a saga deve estar {string}")
    public void statusSaga(String status) {
        assertThat(sagas.findByServiceOrderId(current.getId()).orElseThrow().getStatus().name()).isEqualTo(status);
    }

    @Então("o motivo do cancelamento deve conter {string}")
    public void motivo(String text) {
        assertThat(reload().getCancellationReason()).contains(text);
    }

    @Então("o histórico da OS deve conter {int} registros")
    public void historico(int count) {
        assertThat(history.findByServiceOrderId(current.getId())).hasSize(count);
    }

    private SagaMessage message(String type, Map<String, Object> data) {
        return SagaMessage.of(type, current.getId(), current.getNumber(), data);
    }

    private ServiceOrder reload() {
        return orders.findById(current.getId()).orElseThrow();
    }
}
