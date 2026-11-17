package com.pedrocmoreira.garage.os.infrastructure.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.pedrocmoreira.garage.os.domain.saga.MessageTypes.*;

/**
 * Topologia RabbitMQ: exchange topic "garage.saga" (comandos e eventos) e DLX para mensagens com falha.
 * O OS Service consome todos os eventos ("event.#") dos serviços participantes.
 */
@Configuration
public class RabbitConfig {

    @Bean
    TopicExchange sagaExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    TopicExchange deadLetterExchange() {
        return ExchangeBuilder.topicExchange(DEAD_LETTER_EXCHANGE).durable(true).build();
    }

    @Bean
    Queue osEventsQueue() {
        return QueueBuilder.durable(QUEUE_OS_EVENTS)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(QUEUE_OS_EVENTS + ".dlq")
                .build();
    }

    @Bean
    Queue osEventsDlq() {
        return QueueBuilder.durable(QUEUE_OS_EVENTS + ".dlq").build();
    }

    @Bean
    Binding osEventsBinding() {
        return BindingBuilder.bind(osEventsQueue()).to(sagaExchange()).with("event.#");
    }

    @Bean
    Binding osEventsDlqBinding() {
        return BindingBuilder.bind(osEventsDlq()).to(deadLetterExchange()).with(QUEUE_OS_EVENTS + ".dlq");
    }

    @Bean
    MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
