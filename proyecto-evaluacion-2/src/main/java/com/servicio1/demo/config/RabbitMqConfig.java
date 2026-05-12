package com.servicio1.demo.config;

import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Value("${app.rabbitmq.queue.pos}")
    private String posQueueName;

    // Declara la cola como durable=true para que persista al reiniciar RabbitMQ
    @Bean
    public Queue posQueue() {
        return new Queue(posQueueName, true);
    }
}
