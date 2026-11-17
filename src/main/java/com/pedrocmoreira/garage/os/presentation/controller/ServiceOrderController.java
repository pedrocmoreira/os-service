package com.pedrocmoreira.garage.os.presentation.controller;

import com.pedrocmoreira.garage.os.application.CreateServiceOrderUseCase;
import com.pedrocmoreira.garage.os.application.ServiceOrderSagaOrchestrator;
import com.pedrocmoreira.garage.os.application.StatusChangeService;
import com.pedrocmoreira.garage.os.domain.exception.EntityNotFoundException;
import com.pedrocmoreira.garage.os.domain.model.ServiceOrder;
import com.pedrocmoreira.garage.os.domain.model.StatusSO;
import com.pedrocmoreira.garage.os.domain.repository.HistoryRepository;
import com.pedrocmoreira.garage.os.domain.repository.SagaRepository;
import com.pedrocmoreira.garage.os.domain.repository.ServiceOrderRepository;
import com.pedrocmoreira.garage.os.presentation.dto.ServiceOrderDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/service-orders")
@RequiredArgsConstructor
@Tag(name = "Ordens de Serviço", description = "Abertura, consulta, histórico e acompanhamento da Saga")
public class ServiceOrderController {
    private final CreateServiceOrderUseCase createServiceOrderUseCase;
    private final ServiceOrderSagaOrchestrator orchestrator;
    private final StatusChangeService statusChangeService;
    private final ServiceOrderRepository serviceOrderRepository;
    private final HistoryRepository historyRepository;
    private final SagaRepository sagaRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Abrir OS (inicia a Saga: diagnóstico -> orçamento -> aprovação -> pagamento -> execução)")
    public ServiceOrderDTO.Response create(@Valid @RequestBody ServiceOrderDTO.Request request) {
        var parts = request.parts() == null ? null : request.parts().stream()
                .map(p -> new CreateServiceOrderUseCase.PartItemInput(p.partId(), p.quantity()))
                .toList();
        var input = new CreateServiceOrderUseCase.Input(request.customerId(), request.vehicleId(),
                request.serviceIds(), parts, request.observations());
        return ServiceOrderDTO.Response.from(createServiceOrderUseCase.execute(input));
    }

    @GetMapping
    @Operation(summary = "Listar OS (opcionalmente filtrando por status)")
    public List<ServiceOrderDTO.Response> list(@RequestParam(required = false) StatusSO status) {
        List<ServiceOrder> orders = status == null ? serviceOrderRepository.findAll() : serviceOrderRepository.findByStatus(status);
        return orders.stream().map(ServiceOrderDTO.Response::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar OS por ID")
    public ServiceOrderDTO.Response get(@PathVariable Long id) {
        return ServiceOrderDTO.Response.from(find(id));
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Histórico de status da OS")
    public List<ServiceOrderDTO.HistoryResponse> history(@PathVariable Long id) {
        find(id);
        return historyRepository.findByServiceOrderId(id).stream().map(ServiceOrderDTO.HistoryResponse::from).toList();
    }

    @GetMapping("/{id}/saga")
    @Operation(summary = "Estado da Saga da OS (etapa atual, compensações)")
    public ServiceOrderDTO.SagaResponse saga(@PathVariable Long id) {
        return sagaRepository.findByServiceOrderId(id)
                .map(ServiceOrderDTO.SagaResponse::from)
                .orElseThrow(() -> new EntityNotFoundException("Saga da OS", id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancelar OS (dispara compensações da Saga)")
    public ServiceOrderDTO.Response cancel(@PathVariable Long id, @RequestBody(required = false) ServiceOrderDTO.CancelRequest request) {
        return ServiceOrderDTO.Response.from(orchestrator.cancel(id, request == null ? null : request.reason()));
    }

    @PatchMapping("/{id}/deliver")
    @Transactional
    @Operation(summary = "Registrar entrega do veículo (FINALIZADA -> ENTREGUE)")
    public ServiceOrderDTO.Response deliver(@PathVariable Long id) {
        return ServiceOrderDTO.Response.from(statusChangeService.change(find(id), StatusSO.ENTREGUE, "Veículo entregue ao cliente"));
    }

    @GetMapping("/report/average-time")
    @Operation(summary = "Tempo médio de execução das OS finalizadas/entregues")
    public ServiceOrderDTO.AverageTimeResponse averageTime() {
        List<Long> times = Stream.concat(
                        serviceOrderRepository.findByStatus(StatusSO.FINALIZADA).stream(),
                        serviceOrderRepository.findByStatus(StatusSO.ENTREGUE).stream())
                .map(ServiceOrder::getExecutionTimeInMinutes)
                .filter(Objects::nonNull)
                .toList();
        if (times.isEmpty()) {
            return new ServiceOrderDTO.AverageTimeResponse(0, null, null);
        }
        double avg = times.stream().mapToLong(Long::longValue).average().orElse(0);
        return new ServiceOrderDTO.AverageTimeResponse(times.size(), avg, avg / 60);
    }

    private ServiceOrder find(Long id) {
        return serviceOrderRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Ordem de serviço", id));
    }
}
