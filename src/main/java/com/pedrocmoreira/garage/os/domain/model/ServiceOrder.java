package com.pedrocmoreira.garage.os.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "service_orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ServiceOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String number;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatusSO status = StatusSO.RECEBIDA;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalValue = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @Column(columnDefinition = "TEXT")
    private String diagnosisNotes;

    @Column(length = 500)
    private String paymentUrl;

    @Column(length = 500)
    private String cancellationReason;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime diagnosisStartDate;
    private LocalDateTime executionStartDate;
    private LocalDateTime completionDate;
    private LocalDateTime deliveryDate;

    @OneToMany(mappedBy = "serviceOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @Builder.Default
    private List<ServiceItem> serviceItems = new ArrayList<>();

    @OneToMany(mappedBy = "serviceOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @Builder.Default
    private List<PartItem> partItems = new ArrayList<>();

    /** Aplica a transição validada pela máquina de estados e registra o timestamp da etapa. */
    public StatusSO changeStatus(StatusSO newStatus) {
        StatusSO previous = this.status;
        this.status = previous.transitionTo(newStatus);
        LocalDateTime now = LocalDateTime.now();
        switch (newStatus) {
            case EM_DIAGNOSTICO -> this.diagnosisStartDate = now;
            case EM_EXECUCAO -> this.executionStartDate = now;
            case FINALIZADA -> this.completionDate = now;
            case ENTREGUE -> this.deliveryDate = now;
            default -> { /* sem timestamp específico */ }
        }
        return previous;
    }

    public void addServiceItem(ServiceItem item) {
        item.setServiceOrder(this);
        serviceItems.add(item);
        recalculateTotal();
    }

    public void addPartItem(PartItem item) {
        item.setServiceOrder(this);
        partItems.add(item);
        recalculateTotal();
    }

    public void recalculateTotal() {
        BigDecimal services = serviceItems.stream().map(ServiceItem::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal parts = partItems.stream().map(PartItem::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        this.totalValue = services.add(parts);
    }

    public Long getExecutionTimeInMinutes() {
        if (executionStartDate == null || completionDate == null) return null;
        return Duration.between(executionStartDate, completionDate).toMinutes();
    }
}
