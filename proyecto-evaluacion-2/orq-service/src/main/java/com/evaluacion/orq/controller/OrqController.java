package com.evaluacion.orq.controller;

import com.evaluacion.orq.model.DataResponse;
import com.evaluacion.orq.strategy.ProcessingContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// PATRON STRATEGY: el controlador delega el algoritmo al ProcessingContext.
@RestController
@RequestMapping("/api")
public class OrqController {

    private final ProcessingContext processingContext;
    private final RestTemplate restTemplate;
    private final String dataMsUrl;
    private final String dataMs2Url;

    public OrqController(
            ProcessingContext processingContext,
            @Value("${data.ms.url}") String dataMsUrl,
            @Value("${data.ms2.url}") String dataMs2Url) {
        this.processingContext = processingContext;
        this.restTemplate = new RestTemplate();
        this.dataMsUrl = dataMsUrl;
        this.dataMs2Url = dataMs2Url;
    }

    // GET /api/data?id={requestId}&strategy={batch|stream|cache}
    @GetMapping("/data")
    public ResponseEntity<DataResponse> getData(
            @RequestParam(value = "id", defaultValue = "default") String requestId,
            @RequestParam(value = "strategy", defaultValue = "batch") String strategy) {
        try {
            processingContext.setStrategy(strategy);

            // Llamadas paralelas a MS1 (POS) y MS2 (online)
            CompletableFuture<List<Map<String, Object>>> futureMs1 =
                    CompletableFuture.supplyAsync(() -> fetchFromMs("/api/pos/data", dataMsUrl, "pos"));

            CompletableFuture<List<Map<String, Object>>> futureMs2 =
                    CompletableFuture.supplyAsync(() -> fetchFromMs("/api/online/ventas?dias=7", dataMs2Url, "online"));

            List<Map<String, Object>> transactions = Stream.concat(
                    futureMs1.join().stream(),
                    futureMs2.join().stream()
            ).collect(Collectors.toList());

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

    private List<Map<String, Object>> fetchFromMs(String path, String baseUrl, String canal) {
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    baseUrl + path,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );
            List<Map<String, Object>> body = response.getBody();
            if (body == null) return Collections.emptyList();
            // Inyectar campo "canal" si no viene del modelo
            body.forEach(t -> t.putIfAbsent("canal", canal));
            return body;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
