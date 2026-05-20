package com.servicio1.demo.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servicio1.demo.dto.PosTransactionDto;
import com.servicio1.demo.service.PosProcessingService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PosMessageListener {

    private final PosProcessingService posProcessingService;
    private final ObjectMapper objectMapper;

    public PosMessageListener(PosProcessingService posProcessingService) {
        this.posProcessingService = posProcessingService;
        this.objectMapper = new ObjectMapper();
    }

    @RabbitListener(queues = "${app.rabbitmq.queue.pos}")
    public void receiveMessage(String message) {
        System.out.println("Mensaje recibido de RabbitMQ: " + message);
        try {
            // Conversión de JSON string a DTO
            PosTransactionDto dto = objectMapper.readValue(message, PosTransactionDto.class);
            // Iniciar limpieza y guardado
            posProcessingService.procesarYGuardar(dto);
        } catch (Exception e) {
            System.err.println("Error procesando mensaje RabbitMQ. Data defectuosa o mal format. " + e.getMessage());
            // Si el JSON viene mal, aquí podríamos guardarlo en una tabla de 'dead letter' o mensajes fallidos.
        }
    }
}