package com.evaluacion.bff.controller;

import com.evaluacion.bff.model.DataResponse;
import com.evaluacion.bff.proxy.ServiceProxy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// PATRÓN PROXY: Justificación técnica para evaluación parcial 2
// Client en la estructura del patrón. Solo conoce ServiceProxy;
// no sabe si habla con el servicio real o un intermediario.
// Responsabilidad única: mapear HTTP ↔ llamada al proxy.
@RestController
@RequestMapping("/api/proxy")
public class BffController {

    private final ServiceProxy serviceProxy;

    public BffController(ServiceProxy serviceProxy) {
        this.serviceProxy = serviceProxy;
    }

    // GET /api/proxy/data?id={requestId}
    @GetMapping("/data")
    public ResponseEntity<DataResponse> getData(
            @RequestHeader(value = "Authorization", required = false) String authToken,
            @RequestParam(value = "id", defaultValue = "default") String requestId) {
        try {
            DataResponse response = serviceProxy.fetchData(requestId, authToken);
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(DataResponse.error(e.getMessage()));
        }
    }

    // GET /api/proxy/health
    @GetMapping("/health")
    public ResponseEntity<DataResponse> health() {
        return ResponseEntity.ok(DataResponse.ok("BFF operativo", "bff-service"));
    }
}
