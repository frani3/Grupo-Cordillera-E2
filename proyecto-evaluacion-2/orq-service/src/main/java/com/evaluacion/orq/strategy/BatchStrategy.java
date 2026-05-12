package com.evaluacion.orq.strategy;

import org.springframework.stereotype.Component;

@Component
public class BatchStrategy implements ProcessingStrategy {

    @Override
    public String process(String requestId, Object rawData) {
        return String.format("BATCH[%s]: procesamiento en lote de %d registros",
                requestId, rawData != null ? rawData.toString().length() : 0);
    }

    @Override
    public String getName() {
        return "batch";
    }
}
