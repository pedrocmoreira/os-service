package com.pedrocmoreira.garage.os.presentation.controller;

import com.pedrocmoreira.garage.os.domain.exception.EntityNotFoundException;
import com.pedrocmoreira.garage.os.domain.model.CatalogPart;
import com.pedrocmoreira.garage.os.domain.model.CatalogService;
import com.pedrocmoreira.garage.os.infrastructure.persistence.CatalogPartJpaRepository;
import com.pedrocmoreira.garage.os.infrastructure.persistence.CatalogServiceJpaRepository;
import com.pedrocmoreira.garage.os.presentation.dto.CatalogDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Catálogo comercial de serviços e peças (preços). Estoque fica no Execution Service. */
@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
@Tag(name = "Catálogo")
public class CatalogController {
    private final CatalogServiceJpaRepository serviceRepository;
    private final CatalogPartJpaRepository partRepository;

    @GetMapping("/services")
    @Operation(summary = "Listar serviços")
    public List<CatalogDTO.ServiceResponse> listServices() {
        return serviceRepository.findAll().stream().map(CatalogDTO.ServiceResponse::from).toList();
    }

    @PostMapping("/services")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar serviço")
    public CatalogDTO.ServiceResponse createService(@Valid @RequestBody CatalogDTO.ServiceRequest request) {
        return CatalogDTO.ServiceResponse.from(serviceRepository.save(CatalogService.builder()
                .name(request.name()).description(request.description())
                .basePrice(request.basePrice()).estimatedTimeMinutes(request.estimatedTimeMinutes())
                .build()));
    }

    @PutMapping("/services/{id}")
    @Operation(summary = "Atualizar serviço")
    public CatalogDTO.ServiceResponse updateService(@PathVariable Long id, @Valid @RequestBody CatalogDTO.ServiceRequest request) {
        CatalogService service = serviceRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Serviço", id));
        service.setName(request.name());
        service.setDescription(request.description());
        service.setBasePrice(request.basePrice());
        service.setEstimatedTimeMinutes(request.estimatedTimeMinutes());
        return CatalogDTO.ServiceResponse.from(serviceRepository.save(service));
    }

    @GetMapping("/parts")
    @Operation(summary = "Listar peças")
    public List<CatalogDTO.PartResponse> listParts() {
        return partRepository.findAll().stream().map(CatalogDTO.PartResponse::from).toList();
    }

    @PostMapping("/parts")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar peça (preço). O estoque é cadastrado no Execution Service com o mesmo ID.")
    public CatalogDTO.PartResponse createPart(@Valid @RequestBody CatalogDTO.PartRequest request) {
        return CatalogDTO.PartResponse.from(partRepository.save(CatalogPart.builder()
                .name(request.name()).description(request.description())
                .code(request.code()).unitPrice(request.unitPrice())
                .build()));
    }

    @PutMapping("/parts/{id}")
    @Operation(summary = "Atualizar peça")
    public CatalogDTO.PartResponse updatePart(@PathVariable Long id, @Valid @RequestBody CatalogDTO.PartRequest request) {
        CatalogPart part = partRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Peça", id));
        part.setName(request.name());
        part.setDescription(request.description());
        part.setCode(request.code());
        part.setUnitPrice(request.unitPrice());
        return CatalogDTO.PartResponse.from(partRepository.save(part));
    }
}
