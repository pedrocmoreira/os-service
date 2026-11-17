package com.pedrocmoreira.garage.os.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedMessageJpaRepository extends JpaRepository<ProcessedMessage, String> {
}
