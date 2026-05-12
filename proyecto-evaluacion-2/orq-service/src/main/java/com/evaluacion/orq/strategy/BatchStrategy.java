package com.evaluacion.orq.strategy;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class BatchStrategy implements ProcessingStrategy {

    @Override
    public String process(String requestId, List<Map<String, Object>> transactions) {
        if (transactions.isEmpty()) {
            return String.format("BATCH[%s]: sin transacciones en el data-ms", requestId);
        }
        double total = transactions.stream()
                .mapToDouble(t -> t.get("montoTotal") instanceof Number n ? n.doubleValue() : 0)
                .sum();
        String ids = transactions.stream()
                .map(t -> String.valueOf(t.getOrDefault("transactionId", "?")))
                .reduce((a, b) -> a + ", " + b).orElse("-");
        return String.format("BATCH[%s]: %d transacciones | total=$%.0f | ids=[%s]",
                requestId, transactions.size(), total, ids);
    }

    @Override
    public String getName() { return "batch"; }
}
