package com.pedrocmoreira.garage.os.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "service_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ServiceItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_order_id", nullable = false)
    private ServiceOrder serviceOrder;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "service_id", nullable = false)
    private CatalogService service;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal appliedPrice;

    public BigDecimal getSubtotal() {
        return appliedPrice;
    }
}
