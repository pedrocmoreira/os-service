package com.pedrocmoreira.garage.os.presentation.controller;

import com.pedrocmoreira.garage.os.domain.exception.EntityNotFoundException;
import com.pedrocmoreira.garage.os.domain.model.Customer;
import com.pedrocmoreira.garage.os.domain.model.Vehicle;
import com.pedrocmoreira.garage.os.domain.validator.PlateValidator;
import com.pedrocmoreira.garage.os.infrastructure.persistence.CustomerJpaRepository;
import com.pedrocmoreira.garage.os.infrastructure.persistence.VehicleJpaRepository;
import com.pedrocmoreira.garage.os.presentation.dto.VehicleDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
@Tag(name = "Veículos")
public class VehicleController {
    private final VehicleJpaRepository repository;
    private final CustomerJpaRepository customerRepository;

    @GetMapping
    @Operation(summary = "Listar veículos (opcionalmente por cliente)")
    public List<VehicleDTO.Response> list(@RequestParam(required = false) Long customerId) {
        List<Vehicle> vehicles = customerId == null ? repository.findAll() : repository.findByCustomerId(customerId);
        return vehicles.stream().map(VehicleDTO.Response::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar veículo por ID")
    public VehicleDTO.Response get(@PathVariable Long id) {
        return VehicleDTO.Response.from(find(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar veículo")
    public VehicleDTO.Response create(@Valid @RequestBody VehicleDTO.Request request) {
        String plate = request.plate().toUpperCase().replace("-", "").trim();
        if (!PlateValidator.isValid(plate)) {
            throw new IllegalArgumentException("Placa inválida: " + request.plate());
        }
        if (repository.existsByPlate(plate)) {
            throw new IllegalArgumentException("Já existe um veículo com essa placa.");
        }
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new EntityNotFoundException("Cliente", request.customerId()));
        Vehicle vehicle = Vehicle.builder()
                .plate(plate).make(request.make()).model(request.model())
                .year(request.year()).color(request.color()).customer(customer)
                .build();
        return VehicleDTO.Response.from(repository.save(vehicle));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar veículo")
    public VehicleDTO.Response update(@PathVariable Long id, @Valid @RequestBody VehicleDTO.Request request) {
        Vehicle vehicle = find(id);
        vehicle.setMake(request.make());
        vehicle.setModel(request.model());
        vehicle.setYear(request.year());
        vehicle.setColor(request.color());
        return VehicleDTO.Response.from(repository.save(vehicle));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remover veículo")
    public void delete(@PathVariable Long id) {
        repository.delete(find(id));
    }

    private Vehicle find(Long id) {
        return repository.findById(id).orElseThrow(() -> new EntityNotFoundException("Veículo", id));
    }
}
