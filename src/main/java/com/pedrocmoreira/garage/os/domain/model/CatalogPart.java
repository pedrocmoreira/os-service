package com.pedrocmoreira.garage.os.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Peça do catálogo (dados comerciais/preço). O controle de estoque pertence ao Execution Service.
 */
@Entity
@Table(name = "parts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CatalogPart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(length = 50)
    private String code;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Builder.Default
    private Boolean active = true;
}
