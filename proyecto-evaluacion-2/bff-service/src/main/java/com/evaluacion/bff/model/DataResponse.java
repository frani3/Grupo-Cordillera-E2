package com.evaluacion.bff.model;

import java.time.Instant;

// PATRÓN PROXY: DTO compartido entre todas las capas del BFF.
public record DataResponse(String status, String data, String source, String timestamp) {

    public static DataResponse ok(String data, String source) {
        return new DataResponse("ok", data, source, Instant.now().toString());
    }

    public static DataResponse error(String message) {
        return new DataResponse("error", message, "bff-service", Instant.now().toString());
    }
}
