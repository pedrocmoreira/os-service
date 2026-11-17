package com.pedrocmoreira.garage.os.presentation.dto;

import com.pedrocmoreira.garage.os.domain.model.ServiceOrder;
import com.pedrocmoreira.garage.os.domain.model.ServiceOrderHistory;
import com.pedrocmoreira.garage.os.domain.model.StatusSO;
import com.pedrocmoreira.garage.os.domain.saga.SagaInstance;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class ServiceOrderDTO {
    private ServiceOrderDTO() {}

    public record Request(@NotNull Long customerId, @NotNull Long vehicleId, @NotEmpty List<Long> serviceIds,
                          List<@Valid PartItemRequest> parts, String observations) {}

    public record PartItemRequest(@NotNull Long partId, @NotNull @Positive Integer quantity) {}

    public record CancelRequest(String reason) {}

    public record ServiceItemResponse(Long serviceId, String name, BigDecimal price) {}

    public record PartItemResponse(Long partId, String name, Integer quantity, BigDecimal unitPrice, BigDecimal subtotal) {}

    public record Response(Long id, String number, StatusSO status, BigDecimal totalValue, String observations,
                           String diagnosisNotes, String paymentUrl, String cancellationReason,
                           LocalDateTime createdAt, LocalDateTime completionDate, LocalDateTime deliveryDate,
                           Long executionTimeInMinutes, Long customerId, String customerName,
                           Long vehicleId, String vehiclePlate,
                           List<ServiceItemResponse> services, List<PartItemResponse> parts) {
        public static Response from(ServiceOrder so) {
            return new Response(so.getId(), so.getNumber(), so.getStatus(), so.getTotalValue(), so.getObservations(),
                    so.getDiagnosisNotes(), so.getPaymentUrl(), so.getCancellationReason(),
                    so.getCreatedAt(), so.getCompletionDate(), so.getDeliveryDate(), so.getExecutionTimeInMinutes(),
                    so.getCustomer().getId(), so.getCustomer().getName(),
                    so.getVehicle().getId(), so.getVehicle().getPlate(),
                    so.getServiceItems().stream().map(i -> new ServiceItemResponse(i.getService().getId(), i.getService().getName(), i.getAppliedPrice())).toList(),
                    so.getPartItems().stream().map(i -> new PartItemResponse(i.getPart().getId(), i.getPart().getName(), i.getQuantity(), i.getUnitPriceApplied(), i.getSubtotal())).toList());
        }
    }

    public record HistoryResponse(StatusSO from, StatusSO to, String note, LocalDateTime changedAt) {
        public static HistoryResponse from(ServiceOrderHistory h) {
            return new HistoryResponse(h.getFromStatus(), h.getToStatus(), h.getNote(), h.getChangedAt());
        }
    }

    public record SagaResponse(String sagaId, String status, String currentStep, boolean budgetCreated,
                               boolean paymentConfirmed, boolean partsReserved, String failureReason,
                               LocalDateTime startedAt, LocalDateTime updatedAt) {
        public static SagaResponse from(SagaInstance s) {
            return new SagaResponse(s.getId(), s.getStatus().name(), s.getCurrentStep().name(), s.isBudgetCreated(),
                    s.isPaymentConfirmed(), s.isPartsReserved(), s.getFailureReason(), s.getStartedAt(), s.getUpdatedAt());
        }
    }

    public record PublicStatusResponse(String number, StatusSO status, String vehiclePlate, BigDecimal totalValue,
                                       String paymentUrl, List<HistoryResponse> history) {}

    public record AverageTimeResponse(long completedOrders, Double averageMinutes, Double averageHours) {}
}
