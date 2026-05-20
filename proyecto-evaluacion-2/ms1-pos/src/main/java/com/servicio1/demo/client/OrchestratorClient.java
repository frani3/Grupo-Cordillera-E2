package com.servicio1.demo.client;

import com.servicio1.demo.dto.PosTransactionDto;
import com.servicio1.demo.model.PosTransaction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;

@Service
public class OrchestratorClient {

    private final RestTemplate restTemplate;
    
    @Value("${app.orquestador.url}")
    private String orquestadorUrl;

    public OrchestratorClient() {
        this.restTemplate = new RestTemplate();
    }

    @CircuitBreaker(name = "orquestadorCB", fallbackMethod = "enviarOrquestadorFallback")
    public String enviarAlOrquestador(PosTransaction trx) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("transactionId", trx.getTransactionId());
        payload.put("sucursal", trx.getSucursal());
        payload.put("monto", trx.getMontoTotal());
        payload.put("fecha", trx.getFecha());
        
        ResponseEntity<String> response = restTemplate.postForEntity(orquestadorUrl, payload, String.class);
        return "ORQUESTADOR_OK - " + response.getStatusCode();
    }

    // Fallback: si falla el orquestador, avisamos agregando las flags
    public String enviarOrquestadorFallback(PosTransaction trx, Throwable t) {
        // En un caso real podrías enviar un mensaje de auditoría (MQ-BDAUD-1) a RabbitMQ o guardarlo con una flag de datos desactualizados (Stale Data)
        System.err.println("FALLO SISTEMA EXTERNO: Activando Circuit Breaker para transacción " + trx.getTransactionId());
        return "FALLO_ORQUESTADOR_CACHE - StaleData: true, Timestamp: " + LocalDateTime.now().toString() + " - Reason: " + t.getMessage();
    }
}
