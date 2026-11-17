package com.pedrocmoreira.garage.os.application;

import com.pedrocmoreira.garage.os.domain.exception.EntityNotFoundException;
import com.pedrocmoreira.garage.os.domain.model.*;
import com.pedrocmoreira.garage.os.domain.repository.ServiceOrderRepository;
import com.pedrocmoreira.garage.os.infrastructure.persistence.CatalogPartJpaRepository;
import com.pedrocmoreira.garage.os.infrastructure.persistence.CatalogServiceJpaRepository;
import com.pedrocmoreira.garage.os.infrastructure.persistence.CustomerJpaRepository;
import com.pedrocmoreira.garage.os.infrastructure.persistence.VehicleJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Abertura da OS: valida dados, calcula total e inicia a Saga. */
@Service
@RequiredArgsConstructor
public class CreateServiceOrderUseCase {
    private final ServiceOrderRepository serviceOrderRepository;
    private final CustomerJpaRepository customerRepository;
    private final VehicleJpaRepository vehicleRepository;
    private final CatalogServiceJpaRepository catalogServiceRepository;
    private final CatalogPartJpaRepository catalogPartRepository;
    private final StatusChangeService statusChangeService;
    private final ServiceOrderSagaOrchestrator orchestrator;

    public record PartItemInput(Long partId, int quantity) {}

    public record Input(Long customerId, Long vehicleId, List<Long> serviceIds, List<PartItemInput> parts, String observations) {}

    @Transactional
    public ServiceOrder execute(Input input) {
        Customer customer = customerRepository.findById(input.customerId())
                .orElseThrow(() -> new EntityNotFoundException("Cliente", input.customerId()));
        Vehicle vehicle = vehicleRepository.findById(input.vehicleId())
                .orElseThrow(() -> new EntityNotFoundException("Veículo", input.vehicleId()));

        if (vehicle.getCustomer() != null && !vehicle.getCustomer().getId().equals(customer.getId())) {
            throw new IllegalArgumentException("O veículo informado não pertence ao cliente.");
        }
        if (input.serviceIds() == null || input.serviceIds().isEmpty()) {
            throw new IllegalArgumentException("Uma Ordem de Serviço deve ter ao menos um serviço.");
        }

        ServiceOrder serviceOrder = ServiceOrder.builder()
                .number(serviceOrderRepository.nextNumber())
                .customer(customer)
                .vehicle(vehicle)
                .status(StatusSO.RECEBIDA)
                .observations(input.observations())
                .build();

        for (Long serviceId : input.serviceIds()) {
            CatalogService service = catalogServiceRepository.findById(serviceId)
                    .orElseThrow(() -> new EntityNotFoundException("Serviço", serviceId));
            serviceOrder.addServiceItem(ServiceItem.builder().service(service).appliedPrice(service.getBasePrice()).build());
        }

        if (input.parts() != null) {
            for (PartItemInput partInput : input.parts()) {
                if (partInput.quantity() <= 0) {
                    throw new IllegalArgumentException("Quantidade da peça deve ser maior que zero.");
                }
                CatalogPart part = catalogPartRepository.findById(partInput.partId())
                        .orElseThrow(() -> new EntityNotFoundException("Peça", partInput.partId()));
                serviceOrder.addPartItem(PartItem.builder()
                        .part(part)
                        .quantity(partInput.quantity())
                        .unitPriceApplied(part.getUnitPrice())
                        .build());
            }
        }

        ServiceOrder saved = serviceOrderRepository.save(serviceOrder);
        statusChangeService.recordCreation(saved);
        orchestrator.start(saved);
        return saved;
    }
}
