package com.pedrocmoreira.garage.os.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "part_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_order_id", nullable = false)
    private ServiceOrder serviceOrder;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "part_id", nullable = false)
    private CatalogPart part;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPriceApplied;

    public BigDecimal getSubtotal() {
        return unitPriceApplied.multiply(BigDecimal.valueOf(quantity));
    }
}
