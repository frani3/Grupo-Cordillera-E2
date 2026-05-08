package com.evaluacion.bff.model;

import java.time.Instant;

// DTO inmutable (record Java 17). Jackson lo serializa a JSON automáticamente.
public record DataResponse(String status, String data, String source, String timestamp) {

    public static DataResponse ok(String data, String source) {
        return new DataResponse("ok", data, source, Instant.now().toString());
    }

    public static DataResponse error(String message) {
        return new DataResponse("error", message, "bff-service", Instant.now().toString());
    }
}
