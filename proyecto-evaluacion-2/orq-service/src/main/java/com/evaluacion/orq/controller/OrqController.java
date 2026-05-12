package com.evaluacion.orq.controller;

import com.evaluacion.orq.model.DataResponse;
import com.evaluacion.orq.strategy.ProcessingContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

// PATRÓN STRATEGY: el controlador delega el algoritmo al ProcessingContext.
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

            List<Map<String, Object>> transactions = fetchFromDataMs();
            String result = processingContext.executeStrategy(requestId, transactions);

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

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchFromDataMs() {
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    dataMsUrl + "/api/pos/data",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );
            List<Map<String, Object>> body = response.getBody();
            return body != null ? body : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
