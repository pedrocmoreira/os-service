package com.pedrocmoreira.garage.os.presentation.controller;

import com.pedrocmoreira.garage.os.domain.exception.EntityNotFoundException;
import com.pedrocmoreira.garage.os.domain.model.Customer;
import com.pedrocmoreira.garage.os.domain.validator.DocumentValidator;
import com.pedrocmoreira.garage.os.infrastructure.persistence.CustomerJpaRepository;
import com.pedrocmoreira.garage.os.presentation.dto.CustomerDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "Clientes")
public class CustomerController {
    private final CustomerJpaRepository repository;

    @GetMapping
    @Operation(summary = "Listar clientes")
    public List<CustomerDTO.Response> list() {
        return repository.findAll().stream().map(CustomerDTO.Response::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar cliente por ID")
    public CustomerDTO.Response get(@PathVariable Long id) {
        return CustomerDTO.Response.from(find(id));
    }

    @GetMapping("/document/{document}")
    @Operation(summary = "Buscar cliente por CPF/CNPJ")
    public CustomerDTO.Response getByDocument(@PathVariable String document) {
        return repository.findByDocument(document.replaceAll("\\D", ""))
                .map(CustomerDTO.Response::from)
                .orElseThrow(() -> new EntityNotFoundException("Cliente", document));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar cliente")
    public CustomerDTO.Response create(@Valid @RequestBody CustomerDTO.Request request) {
        if (!DocumentValidator.isValid(request.document())) {
            throw new IllegalArgumentException("CPF ou CNPJ inválido: " + request.document());
        }
        String digits = request.document().replaceAll("\\D", "");
        if (repository.existsByDocument(digits)) {
            throw new IllegalArgumentException("Já existe um cliente com esse documento.");
        }
        Customer customer = Customer.builder()
                .document(digits)
                .documentType(DocumentValidator.detectType(digits))
                .name(request.name())
                .phone(request.phone())
                .email(request.email())
                .build();
        return CustomerDTO.Response.from(repository.save(customer));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar cliente")
    public CustomerDTO.Response update(@PathVariable Long id, @Valid @RequestBody CustomerDTO.Request request) {
        Customer customer = find(id);
        customer.setName(request.name());
        customer.setPhone(request.phone());
        customer.setEmail(request.email());
        return CustomerDTO.Response.from(repository.save(customer));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remover cliente")
    public void delete(@PathVariable Long id) {
        repository.delete(find(id));
    }

    private Customer find(Long id) {
        return repository.findById(id).orElseThrow(() -> new EntityNotFoundException("Cliente", id));
    }
}
