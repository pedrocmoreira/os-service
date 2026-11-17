package com.pedrocmoreira.garage.os.domain.saga;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Estado persistido da Saga de uma OS. Os flags indicam quais etapas já produziram efeitos
 * nos outros serviços e, portanto, precisam ser compensadas em caso de falha.
 */
@Entity
@Table(name = "saga_instances")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SagaInstance {
    @Id
    @Builder.Default
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false, unique = true)
    private Long serviceOrderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SagaStatus status = SagaStatus.STARTED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @Builder.Default
    private SagaStep currentStep = SagaStep.DIAGNOSIS_REQUESTED;

    @Builder.Default
    private boolean budgetCreated = false;

    @Builder.Default
    private boolean paymentConfirmed = false;

    @Builder.Default
    private boolean partsReserved = false;

    @Column(length = 500)
    private String failureReason;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime startedAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    public void moveTo(SagaStep step) {
        this.currentStep = step;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return status == SagaStatus.STARTED;
    }
}
