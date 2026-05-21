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

        Map<String, Object> lastPos = transactions.stream()
                .filter(t -> "pos".equals(t.get("canal")))
                .reduce((a, b) -> b)
                .orElse(null);

        Map<String, Object> lastOnline = transactions.stream()
                .filter(t -> "online".equals(t.get("canal")))
                .reduce((a, b) -> b)
                .orElse(null);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("STREAM[%s]:", requestId));

        if (lastPos != null) {
            sb.append(String.format(" POS ultima=%s | sucursal=%s | monto=$%.0f",
                    lastPos.getOrDefault("transactionId", "?"),
                    lastPos.getOrDefault("sucursal", "?"),
                    lastPos.get("montoTotal") instanceof Number n ? n.doubleValue() : 0));
        }
        if (lastOnline != null) {
            sb.append(String.format(" | ONLINE ultima=%s | plataforma=%s | monto=$%.0f",
                    lastOnline.getOrDefault("transactionId", "?"),
                    lastOnline.getOrDefault("plataforma", "?"),
                    lastOnline.get("montoTotal") instanceof Number n ? n.doubleValue() : 0));
        }
        return sb.toString();
    }

    @Override
    public String getName() { return "stream"; }
}
