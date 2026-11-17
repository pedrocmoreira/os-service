package com.pedrocmoreira.garage.os.infrastructure.persistence;

import com.pedrocmoreira.garage.os.domain.repository.ProcessedMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class ProcessedMessageRepositoryAdapter implements ProcessedMessageRepository {
    private final ProcessedMessageJpaRepository jpa;

    @Override
    public boolean registerIfNew(String messageId) {
        if (messageId == null || jpa.existsById(messageId)) {
            return messageId == null;
        }
        jpa.save(new ProcessedMessage(messageId, LocalDateTime.now()));
        return true;
    }
}
