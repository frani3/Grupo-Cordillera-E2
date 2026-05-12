package com.evaluacion.orq.strategy;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class StreamStrategy implements ProcessingStrategy {

    @Override
    public String process(String requestId, List<Map<String, Object>> transactions) {
        if (transactions.isEmpty()) {
            return String.format("STREAM[%s]: sin transacciones en tiempo real", requestId);
        }
        Map<String, Object> last = transactions.get(transactions.size() - 1);
        return String.format("STREAM[%s]: ultima=%s | sucursal=%s | monto=$%.0f",
                requestId,
                last.getOrDefault("transactionId", "?"),
                last.getOrDefault("sucursal", "?"),
                last.get("montoTotal") instanceof Number n ? n.doubleValue() : 0);
    }

    @Override
    public String getName() { return "stream"; }
}
