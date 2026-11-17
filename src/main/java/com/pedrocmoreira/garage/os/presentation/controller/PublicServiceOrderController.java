package com.pedrocmoreira.garage.os.presentation.controller;

import com.pedrocmoreira.garage.os.domain.exception.EntityNotFoundException;
import com.pedrocmoreira.garage.os.domain.model.ServiceOrder;
import com.pedrocmoreira.garage.os.domain.repository.HistoryRepository;
import com.pedrocmoreira.garage.os.domain.repository.ServiceOrderRepository;
import com.pedrocmoreira.garage.os.presentation.dto.ServiceOrderDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/service-orders")
@RequiredArgsConstructor
@Tag(name = "Consulta pública", description = "Acompanhamento da OS pelo cliente (sem login)")
public class PublicServiceOrderController {
    private final ServiceOrderRepository serviceOrderRepository;
    private final HistoryRepository historyRepository;

    @GetMapping("/{number}")
    @Operation(summary = "Consultar status e histórico da OS pelo número")
    public ServiceOrderDTO.PublicStatusResponse status(@PathVariable String number) {
        ServiceOrder so = serviceOrderRepository.findByNumber(number)
                .orElseThrow(() -> new EntityNotFoundException("Ordem de serviço", number));
        return new ServiceOrderDTO.PublicStatusResponse(so.getNumber(), so.getStatus(), so.getVehicle().getPlate(),
                so.getTotalValue(), so.getPaymentUrl(),
                historyRepository.findByServiceOrderId(so.getId()).stream().map(ServiceOrderDTO.HistoryResponse::from).toList());
    }
}
