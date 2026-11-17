package com.pedrocmoreira.garage.os.infrastructure.persistence;

import com.pedrocmoreira.garage.os.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
}
