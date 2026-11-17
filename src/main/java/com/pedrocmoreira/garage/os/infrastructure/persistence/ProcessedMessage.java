package com.pedrocmoreira.garage.os.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "processed_messages")
@Getter @NoArgsConstructor @AllArgsConstructor
public class ProcessedMessage {
    @Id
    private String messageId;
    private LocalDateTime processedAt;
}
