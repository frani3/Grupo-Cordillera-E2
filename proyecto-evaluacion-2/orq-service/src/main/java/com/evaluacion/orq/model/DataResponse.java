package com.evaluacion.orq.model;

import java.time.Instant;

public record DataResponse(String status, String data, String source, String timestamp) {

    public static DataResponse ok(String data, String source) {
        return new DataResponse("ok", data, source, Instant.now().toString());
    }

    public static DataResponse error(String message) {
        return new DataResponse("error", message, "orq-service", Instant.now().toString());
    }
}
