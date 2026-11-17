package com.pedrocmoreira.garage.os.application;

import com.pedrocmoreira.garage.os.TestFixtures;
import com.pedrocmoreira.garage.os.domain.exception.EntityNotFoundException;
import com.pedrocmoreira.garage.os.domain.model.Customer;
import com.pedrocmoreira.garage.os.domain.model.ServiceOrder;
import com.pedrocmoreira.garage.os.domain.model.StatusSO;
import com.pedrocmoreira.garage.os.domain.model.Vehicle;
import com.pedrocmoreira.garage.os.domain.repository.ServiceOrderRepository;
import com.pedrocmoreira.garage.os.infrastructure.persistence.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreateServiceOrderUseCaseTest {
    private ServiceOrderRepository serviceOrderRepository;
    private CustomerJpaRepository customerRepository;
    private VehicleJpaRepository vehicleRepository;
    private CatalogServiceJpaRepository catalogServiceRepository;
    private CatalogPartJpaRepository catalogPartRepository;
    private StatusChangeService statusChangeService;
    private ServiceOrderSagaOrchestrator orchestrator;
    private CreateServiceOrderUseCase useCase;
    private Customer customer;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        serviceOrderRepository = mock(ServiceOrderRepository.class);
        customerRepository = mock(CustomerJpaRepository.class);
        vehicleRepository = mock(VehicleJpaRepository.class);
        catalogServiceRepository = mock(CatalogServiceJpaRepository.class);
        catalogPartRepository = mock(CatalogPartJpaRepository.class);
        statusChangeService = mock(StatusChangeService.class);
        orchestrator = mock(ServiceOrderSagaOrchestrator.class);
        useCase = new CreateServiceOrderUseCase(serviceOrderRepository, customerRepository, vehicleRepository,
                catalogServiceRepository, catalogPartRepository, statusChangeService, orchestrator);

        customer = TestFixtures.customer();
        vehicle = TestFixtures.vehicle(customer);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
        when(catalogServiceRepository.findById(1L)).thenReturn(Optional.of(TestFixtures.service()));
        when(catalogPartRepository.findById(2L)).thenReturn(Optional.of(TestFixtures.part()));
        when(serviceOrderRepository.nextNumber()).thenReturn("OS-2026-00001");
        when(serviceOrderRepository.save(any())).thenAnswer(inv -> {
            ServiceOrder so = inv.getArgument(0);
            so.setId(100L);
            return so;
        });
    }

    @Test
    void shouldCreateOrderAndStartSaga() {
        var input = new CreateServiceOrderUseCase.Input(1L, 10L, List.of(1L),
                List.of(new CreateServiceOrderUseCase.PartItemInput(2L, 2)), "obs");

        ServiceOrder created = useCase.execute(input);

        assertThat(created.getNumber()).isEqualTo("OS-2026-00001");
        assertThat(created.getStatus()).isEqualTo(StatusSO.RECEBIDA);
        assertThat(created.getTotalValue()).isEqualByComparingTo("226.00");
        verify(statusChangeService).recordCreation(created);
        verify(orchestrator).start(created);
    }

    @Test
    void shouldCreateOrderWithoutParts() {
        ServiceOrder created = useCase.execute(new CreateServiceOrderUseCase.Input(1L, 10L, List.of(1L), null, null));
        assertThat(created.getPartItems()).isEmpty();
    }

    @Test
    void shouldRequireAtLeastOneService() {
        var input = new CreateServiceOrderUseCase.Input(1L, 10L, List.of(), null, null);
        assertThatThrownBy(() -> useCase.execute(input)).isInstanceOf(IllegalArgumentException.class);
        var nullInput = new CreateServiceOrderUseCase.Input(1L, 10L, null, null, null);
        assertThatThrownBy(() -> useCase.execute(nullInput)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectVehicleFromAnotherCustomer() {
        vehicle.setCustomer(Customer.builder().id(99L).build());
        var input = new CreateServiceOrderUseCase.Input(1L, 10L, List.of(1L), null, null);
        assertThatThrownBy(() -> useCase.execute(input)).hasMessageContaining("não pertence");
    }

    @Test
    void shouldRejectInvalidPartQuantity() {
        var input = new CreateServiceOrderUseCase.Input(1L, 10L, List.of(1L),
                List.of(new CreateServiceOrderUseCase.PartItemInput(2L, 0)), null);
        assertThatThrownBy(() -> useCase.execute(input)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldFailWhenEntitiesDoNotExist() {
        assertThatThrownBy(() -> useCase.execute(new CreateServiceOrderUseCase.Input(5L, 10L, List.of(1L), null, null)))
                .isInstanceOf(EntityNotFoundException.class);
        assertThatThrownBy(() -> useCase.execute(new CreateServiceOrderUseCase.Input(1L, 5L, List.of(1L), null, null)))
                .isInstanceOf(EntityNotFoundException.class);
        assertThatThrownBy(() -> useCase.execute(new CreateServiceOrderUseCase.Input(1L, 10L, List.of(5L), null, null)))
                .isInstanceOf(EntityNotFoundException.class);
        assertThatThrownBy(() -> useCase.execute(new CreateServiceOrderUseCase.Input(1L, 10L, List.of(1L),
                List.of(new CreateServiceOrderUseCase.PartItemInput(5L, 1)), null)))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
