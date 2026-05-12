package com.evaluacion.orq.strategy;

import org.springframework.stereotype.Component;

@Component
public class StreamStrategy implements ProcessingStrategy {

    @Override
    public String process(String requestId, Object rawData) {
        return String.format("STREAM[%s]: procesamiento en tiempo real activado", requestId);
    }

    @Override
    public String getName() {
        return "stream";
    }
}
