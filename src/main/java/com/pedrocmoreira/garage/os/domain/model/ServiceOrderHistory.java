package com.pedrocmoreira.garage.os.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Histórico de mudanças de status da OS (consulta de status e histórico). */
@Entity
@Table(name = "service_order_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ServiceOrderHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long serviceOrderId;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private StatusSO fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusSO toStatus;

    @Column(length = 500)
    private String note;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime changedAt = LocalDateTime.now();
}
