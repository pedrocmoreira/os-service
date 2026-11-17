package com.pedrocmoreira.garage.os.infrastructure.persistence;

import com.pedrocmoreira.garage.os.domain.model.CatalogService;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogServiceJpaRepository extends JpaRepository<CatalogService, Long> {
}
