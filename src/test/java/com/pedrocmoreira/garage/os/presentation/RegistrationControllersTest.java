package com.pedrocmoreira.garage.os.presentation;

import com.pedrocmoreira.garage.os.TestFixtures;
import com.pedrocmoreira.garage.os.domain.model.CatalogPart;
import com.pedrocmoreira.garage.os.domain.model.CatalogService;
import com.pedrocmoreira.garage.os.domain.model.Customer;
import com.pedrocmoreira.garage.os.domain.model.Vehicle;
import com.pedrocmoreira.garage.os.infrastructure.persistence.*;
import com.pedrocmoreira.garage.os.presentation.controller.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RegistrationControllersTest {
    private CustomerJpaRepository customerRepository;
    private VehicleJpaRepository vehicleRepository;
    private CatalogServiceJpaRepository serviceRepository;
    private CatalogPartJpaRepository partRepository;
    private MockMvc mvc;
    private Customer customer;

    @BeforeEach
    void setUp() {
        customerRepository = mock(CustomerJpaRepository.class);
        vehicleRepository = mock(VehicleJpaRepository.class);
        serviceRepository = mock(CatalogServiceJpaRepository.class);
        partRepository = mock(CatalogPartJpaRepository.class);
        mvc = MockMvcBuilders.standaloneSetup(
                        new CustomerController(customerRepository),
                        new VehicleController(vehicleRepository, customerRepository),
                        new CatalogController(serviceRepository, partRepository))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        customer = TestFixtures.customer();
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(serviceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(partRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void customerCrud() throws Exception {
        when(customerRepository.findAll()).thenReturn(List.of(customer));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.findByDocument("52998224725")).thenReturn(Optional.of(customer));

        mvc.perform(get("/api/customers")).andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("João Silva"));
        mvc.perform(get("/api/customers/1")).andExpect(status().isOk());
        mvc.perform(get("/api/customers/document/529.982.247-25")).andExpect(status().isOk());
        mvc.perform(get("/api/customers/2")).andExpect(status().isNotFound());
        mvc.perform(get("/api/customers/document/1")).andExpect(status().isNotFound());

        String body = "{\"document\":\"529.982.247-25\",\"name\":\"Maria\",\"email\":\"maria@email.com\"}";
        mvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documentType").value("CPF"));

        mvc.perform(put("/api/customers/1").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Maria"));

        mvc.perform(delete("/api/customers/1")).andExpect(status().isNoContent());
        verify(customerRepository).delete(customer);
    }

    @Test
    void customerValidation() throws Exception {
        mvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"document\":\"123\",\"name\":\"X\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content("{\"document\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Erro de validação"));
        when(customerRepository.existsByDocument("52998224725")).thenReturn(true);
        mvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"document\":\"52998224725\",\"name\":\"X\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void vehicleCrud() throws Exception {
        Vehicle vehicle = TestFixtures.vehicle(customer);
        when(vehicleRepository.findAll()).thenReturn(List.of(vehicle));
        when(vehicleRepository.findByCustomerId(1L)).thenReturn(List.of(vehicle));
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        mvc.perform(get("/api/vehicles")).andExpect(status().isOk()).andExpect(jsonPath("$[0].plate").value("ABC1D23"));
        mvc.perform(get("/api/vehicles?customerId=1")).andExpect(status().isOk());
        mvc.perform(get("/api/vehicles/10")).andExpect(status().isOk());
        mvc.perform(get("/api/vehicles/11")).andExpect(status().isNotFound());

        String body = "{\"plate\":\"xyz-1234\",\"make\":\"VW\",\"model\":\"Gol\",\"year\":2019,\"customerId\":1}";
        mvc.perform(post("/api/vehicles").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.plate").value("XYZ1234"));
        mvc.perform(put("/api/vehicles/10").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.model").value("Gol"));
        mvc.perform(delete("/api/vehicles/10")).andExpect(status().isNoContent());

        mvc.perform(post("/api/vehicles").contentType(MediaType.APPLICATION_JSON)
                        .content(body.replace("xyz-1234", "12")))
                .andExpect(status().isBadRequest());
        when(vehicleRepository.existsByPlate("XYZ1234")).thenReturn(true);
        mvc.perform(post("/api/vehicles").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void catalogCrud() throws Exception {
        CatalogService service = TestFixtures.service();
        CatalogPart part = TestFixtures.part();
        when(serviceRepository.findAll()).thenReturn(List.of(service));
        when(partRepository.findAll()).thenReturn(List.of(part));
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(service));
        when(partRepository.findById(2L)).thenReturn(Optional.of(part));

        mvc.perform(get("/api/catalog/services")).andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("Troca de óleo"));
        mvc.perform(get("/api/catalog/parts")).andExpect(status().isOk());

        String serviceBody = "{\"name\":\"Lavagem\",\"basePrice\":50.0,\"estimatedTimeMinutes\":30}";
        mvc.perform(post("/api/catalog/services").contentType(MediaType.APPLICATION_JSON).content(serviceBody))
                .andExpect(status().isCreated());
        mvc.perform(put("/api/catalog/services/1").contentType(MediaType.APPLICATION_JSON).content(serviceBody))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Lavagem"));
        mvc.perform(put("/api/catalog/services/9").contentType(MediaType.APPLICATION_JSON).content(serviceBody))
                .andExpect(status().isNotFound());

        String partBody = "{\"name\":\"Vela\",\"code\":\"V1\",\"unitPrice\":20.0}";
        mvc.perform(post("/api/catalog/parts").contentType(MediaType.APPLICATION_JSON).content(partBody))
                .andExpect(status().isCreated());
        mvc.perform(put("/api/catalog/parts/2").contentType(MediaType.APPLICATION_JSON).content(partBody))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("V1"));
        mvc.perform(put("/api/catalog/parts/9").contentType(MediaType.APPLICATION_JSON).content(partBody))
                .andExpect(status().isNotFound());
    }
}
