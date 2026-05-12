package com.evaluacion.orq.controller;

import com.evaluacion.orq.model.DataResponse;
import com.evaluacion.orq.strategy.ProcessingContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

// PATRÓN STRATEGY: Justificación técnica para evaluación parcial 2
// El controlador actúa como cliente del Context.
// Delega la elección del algoritmo al ProcessingContext,
// desacoplando la lógica HTTP de los algoritmos concretos.
@RestController
@RequestMapping("/api")
public class OrqController {

    private final ProcessingContext processingContext;
    private final RestTemplate restTemplate;
    private final String dataMsUrl;

    public OrqController(
            ProcessingContext processingContext,
            @Value("${data.ms.url}") String dataMsUrl) {
        this.processingContext = processingContext;
        this.restTemplate = new RestTemplate();
        this.dataMsUrl = dataMsUrl;
    }

    // GET /api/data?id={requestId}&strategy={batch|stream|cache}
    @GetMapping("/data")
    public ResponseEntity<DataResponse> getData(
            @RequestParam(value = "id", defaultValue = "default") String requestId,
            @RequestParam(value = "strategy", defaultValue = "batch") String strategy) {
        try {
            processingContext.setStrategy(strategy);

            // Obtiene datos crudos del data-ms (modelo PULL)
            String rawData = fetchFromDataMs();

            String result = processingContext.executeStrategy(requestId, rawData);
            return ResponseEntity.ok(DataResponse.ok(result, "orq-service"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(DataResponse.error("Error al orquestar: " + e.getMessage()));
        }
    }

    // POST /api/v1/pos — recibe notificaciones push del data-ms (modelo PUSH legado)
    @PostMapping("/v1/pos")
    public ResponseEntity<String> receivePos(@RequestBody Object payload) {
        return ResponseEntity.ok("Recibido por orq-service: " + payload);
    }

    // GET /api/health
    @GetMapping("/health")
    public ResponseEntity<DataResponse> health() {
        return ResponseEntity.ok(DataResponse.ok("orq-service operativo", "orq-service"));
    }

    private String fetchFromDataMs() {
        try {
            Object[] transactions = restTemplate.getForObject(
                    dataMsUrl + "/api/pos/data", Object[].class);
            return transactions != null ? "count=" + transactions.length : "count=0";
        } catch (Exception e) {
            return "data-ms no disponible: " + e.getMessage();
        }
    }
}
