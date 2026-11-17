package com.pedrocmoreira.garage.os.presentation.dto;

import com.pedrocmoreira.garage.os.domain.model.Vehicle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class VehicleDTO {
    private VehicleDTO() {}
    public record Request(@NotBlank String plate, @NotBlank String make, @NotBlank String model,
                          @NotNull Integer year, String color, @NotNull Long customerId) {}
    public record Response(Long id, String plate, String make, String model, Integer year, String color, Long customerId) {
        public static Response from(Vehicle v) {
            return new Response(v.getId(), v.getPlate(), v.getMake(), v.getModel(), v.getYear(), v.getColor(),
                    v.getCustomer() == null ? null : v.getCustomer().getId());
        }
    }
}
