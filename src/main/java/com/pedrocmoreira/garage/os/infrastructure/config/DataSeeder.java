package com.pedrocmoreira.garage.os.infrastructure.config;

import com.pedrocmoreira.garage.os.domain.model.User;
import com.pedrocmoreira.garage.os.infrastructure.persistence.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Cria o usuário administrador a partir das variáveis ADMIN_USERNAME / ADMIN_PASSWORD (.env). */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {
    private final UserJpaRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin.username:}")
    private String adminUsername;

    @Value("${app.seed.admin.password:}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (adminUsername.isBlank() || adminPassword.isBlank()) {
            log.warn("ADMIN_USERNAME/ADMIN_PASSWORD não definidos - usuário admin não será criado");
            return;
        }
        if (!userRepository.existsByUsername(adminUsername)) {
            userRepository.save(User.builder()
                    .username(adminUsername)
                    .password(passwordEncoder.encode(adminPassword))
                    .name("Administrador")
                    .userType(User.UserType.ADMIN)
                    .build());
            log.info("Usuário admin criado");
        }
    }
}
