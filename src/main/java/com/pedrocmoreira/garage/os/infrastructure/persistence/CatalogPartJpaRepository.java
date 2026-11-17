package com.pedrocmoreira.garage.os.infrastructure.persistence;

import com.pedrocmoreira.garage.os.domain.model.CatalogPart;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogPartJpaRepository extends JpaRepository<CatalogPart, Long> {
}
