package com.pedrocmoreira.garage.os.infrastructure.persistence;

import com.pedrocmoreira.garage.os.domain.model.ServiceOrder;
import com.pedrocmoreira.garage.os.domain.model.StatusSO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ServiceOrderJpaRepository extends JpaRepository<ServiceOrder, Long> {
    Optional<ServiceOrder> findByNumber(String number);
    List<ServiceOrder> findByStatus(StatusSO status);

    @Query(value = "SELECT nextval('service_order_number_seq')", nativeQuery = true)
    long nextSequence();
}
