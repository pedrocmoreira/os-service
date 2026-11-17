package com.pedrocmoreira.garage.os.presentation.dto;

import com.pedrocmoreira.garage.os.domain.model.CatalogPart;
import com.pedrocmoreira.garage.os.domain.model.CatalogService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public final class CatalogDTO {
    private CatalogDTO() {}

    public record ServiceRequest(@NotBlank String name, String description, @NotNull @Positive BigDecimal basePrice,
                                 Integer estimatedTimeMinutes) {}
    public record ServiceResponse(Long id, String name, String description, BigDecimal basePrice,
                                  Integer estimatedTimeMinutes, Boolean active) {
        public static ServiceResponse from(CatalogService s) {
            return new ServiceResponse(s.getId(), s.getName(), s.getDescription(), s.getBasePrice(), s.getEstimatedTimeMinutes(), s.getActive());
        }
    }

    public record PartRequest(@NotBlank String name, String description, String code, @NotNull @Positive BigDecimal unitPrice) {}
    public record PartResponse(Long id, String name, String description, String code, BigDecimal unitPrice, Boolean active) {
        public static PartResponse from(CatalogPart p) {
            return new PartResponse(p.getId(), p.getName(), p.getDescription(), p.getCode(), p.getUnitPrice(), p.getActive());
        }
    }
}
