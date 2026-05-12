package com.evaluacion.orq.strategy;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CacheStrategy implements ProcessingStrategy {

    @Override
    public String process(String requestId, List<Map<String, Object>> transactions) {
        if (transactions.isEmpty()) {
            return String.format("CACHE[%s]: sin datos en cache", requestId);
        }
        String resumen = transactions.stream()
                .map(t -> t.getOrDefault("transactionId", "?") + "(" + t.getOrDefault("metodoPago", "?") + ")")
                .collect(Collectors.joining(", "));
        return String.format("CACHE[%s]: %d registros servidos desde cache | %s",
                requestId, transactions.size(), resumen);
    }

    @Override
    public String getName() { return "cache"; }
}
