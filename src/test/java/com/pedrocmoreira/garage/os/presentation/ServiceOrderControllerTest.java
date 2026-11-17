package com.pedrocmoreira.garage.os.presentation;

import com.pedrocmoreira.garage.os.TestFixtures;
import com.pedrocmoreira.garage.os.application.CreateServiceOrderUseCase;
import com.pedrocmoreira.garage.os.application.ServiceOrderSagaOrchestrator;
import com.pedrocmoreira.garage.os.application.StatusChangeService;
import com.pedrocmoreira.garage.os.domain.exception.InvalidStatusTransitionException;
import com.pedrocmoreira.garage.os.domain.model.ServiceOrder;
import com.pedrocmoreira.garage.os.domain.model.ServiceOrderHistory;
import com.pedrocmoreira.garage.os.domain.model.StatusSO;
import com.pedrocmoreira.garage.os.domain.repository.HistoryRepository;
import com.pedrocmoreira.garage.os.domain.repository.SagaRepository;
import com.pedrocmoreira.garage.os.domain.repository.ServiceOrderRepository;
import com.pedrocmoreira.garage.os.domain.saga.SagaInstance;
import com.pedrocmoreira.garage.os.infrastructure.security.JwtTokenProvider;
import com.pedrocmoreira.garage.os.presentation.controller.AuthController;
import com.pedrocmoreira.garage.os.presentation.controller.GlobalExceptionHandler;
import com.pedrocmoreira.garage.os.presentation.controller.PublicServiceOrderController;
import com.pedrocmoreira.garage.os.presentation.controller.ServiceOrderController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ServiceOrderControllerTest {
    private CreateServiceOrderUseCase createUseCase;
    private ServiceOrderSagaOrchestrator orchestrator;
    private StatusChangeService statusChangeService;
    private ServiceOrderRepository repository;
    private HistoryRepository historyRepository;
    private SagaRepository sagaRepository;
    private AuthenticationManager authenticationManager;
    private MockMvc mvc;
    private ServiceOrder so;

    @BeforeEach
    void setUp() {
        createUseCase = mock(CreateServiceOrderUseCase.class);
        orchestrator = mock(ServiceOrderSagaOrchestrator.class);
        statusChangeService = mock(StatusChangeService.class);
        repository = mock(ServiceOrderRepository.class);
        historyRepository = mock(HistoryRepository.class);
        sagaRepository = mock(SagaRepository.class);
        authenticationManager = mock(AuthenticationManager.class);
        JwtTokenProvider tokenProvider = new JwtTokenProvider("segredo-de-teste-com-mais-de-32-caracteres!!", 1000);
        mvc = MockMvcBuilders.standaloneSetup(
                        new ServiceOrderController(createUseCase, orchestrator, statusChangeService, repository, historyRepository, sagaRepository),
                        new PublicServiceOrderController(repository, historyRepository),
                        new AuthController(authenticationManager, tokenProvider))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        so = TestFixtures.serviceOrder(StatusSO.RECEBIDA);
        when(repository.findById(100L)).thenReturn(Optional.of(so));
        when(repository.findByNumber("OS-2026-00001")).thenReturn(Optional.of(so));
        when(historyRepository.findByServiceOrderId(100L)).thenReturn(List.of(
                ServiceOrderHistory.builder().serviceOrderId(100L).toStatus(StatusSO.RECEBIDA).note("aberta").build()));
    }

    @Test
    void createShouldStartSaga() throws Exception {
        when(createUseCase.execute(any())).thenReturn(so);
        String body = "{\"customerId\":1,\"vehicleId\":10,\"serviceIds\":[1],\"parts\":[{\"partId\":2,\"quantity\":4}]}";
        mvc.perform(post("/api/service-orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.number").value("OS-2026-00001"))
                .andExpect(jsonPath("$.totalValue").value(302.0));

        mvc.perform(post("/api/service-orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":1,\"vehicleId\":10,\"serviceIds\":[1]}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/service-orders").contentType(MediaType.APPLICATION_JSON).content("{\"customerId\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void queriesShouldReturnOrders() throws Exception {
        when(repository.findAll()).thenReturn(List.of(so));
        when(repository.findByStatus(StatusSO.RECEBIDA)).thenReturn(List.of(so));
        when(sagaRepository.findByServiceOrderId(100L)).thenReturn(Optional.of(SagaInstance.builder().serviceOrderId(100L).build()));

        mvc.perform(get("/api/service-orders")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(100));
        mvc.perform(get("/api/service-orders?status=RECEBIDA")).andExpect(status().isOk());
        mvc.perform(get("/api/service-orders/100")).andExpect(status().isOk()).andExpect(jsonPath("$.vehiclePlate").value("ABC1D23"));
        mvc.perform(get("/api/service-orders/101")).andExpect(status().isNotFound());
        mvc.perform(get("/api/service-orders/100/history")).andExpect(status().isOk()).andExpect(jsonPath("$[0].to").value("RECEBIDA"));
        mvc.perform(get("/api/service-orders/100/saga")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("STARTED"));
        mvc.perform(get("/api/service-orders/101/saga")).andExpect(status().isNotFound());
        mvc.perform(get("/api/public/service-orders/OS-2026-00001")).andExpect(status().isOk()).andExpect(jsonPath("$.history[0].note").value("aberta"));
        mvc.perform(get("/api/public/service-orders/X")).andExpect(status().isNotFound());
    }

    @Test
    void cancelAndDeliver() throws Exception {
        when(orchestrator.cancel(eq(100L), any())).thenReturn(so);
        when(statusChangeService.change(any(), eq(StatusSO.ENTREGUE), any())).thenReturn(so);

        mvc.perform(post("/api/service-orders/100/cancel").contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"desistiu\"}"))
                .andExpect(status().isOk());
        verify(orchestrator).cancel(100L, "desistiu");
        mvc.perform(post("/api/service-orders/100/cancel")).andExpect(status().isOk());
        mvc.perform(patch("/api/service-orders/100/deliver")).andExpect(status().isOk());

        when(statusChangeService.change(any(), eq(StatusSO.ENTREGUE), any())).thenThrow(new InvalidStatusTransitionException("inválida"));
        mvc.perform(patch("/api/service-orders/100/deliver")).andExpect(status().is(422));
    }

    @Test
    void averageTimeReport() throws Exception {
        when(repository.findByStatus(any())).thenReturn(List.of());
        mvc.perform(get("/api/service-orders/report/average-time")).andExpect(status().isOk()).andExpect(jsonPath("$.completedOrders").value(0));

        so.setExecutionStartDate(LocalDateTime.now().minusMinutes(120));
        so.setCompletionDate(LocalDateTime.now());
        when(repository.findByStatus(StatusSO.FINALIZADA)).thenReturn(List.of(so));
        mvc.perform(get("/api/service-orders/report/average-time"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.averageHours").value(2.0));
    }

    @Test
    void loginShouldReturnToken() throws Exception {
        var principal = User.withUsername("admin").password("x").roles("ADMIN").build();
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"admin\",\"password\":\"x\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.type").value("Bearer"));

        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"admin\",\"password\":\"y\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unexpectedErrorsReturn500() throws Exception {
        when(repository.findAll()).thenThrow(new IllegalStateException("boom"));
        mvc.perform(get("/api/service-orders")).andExpect(status().isInternalServerError());
    }
}
