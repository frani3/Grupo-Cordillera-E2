package com.evaluacion.orq.strategy;

import java.util.HashMap;
import java.util.Map;

// PATRÓN STRATEGY: Justificación técnica para evaluación parcial 2
// ConcreteStrategy C: revisa caché antes de procesar → evita reprocesamiento.
public class CacheProcessingStrategy implements IProcessingStrategy {

    private final Map<String, String> cache = new HashMap<>();

    @Override
    public String process(String data) {
        if (cache.containsKey(data)) {
            System.out.println("[CacheStrategy] Cache HIT para: " + data);
            return cache.get(data);
        }
        System.out.println("[CacheStrategy] Cache MISS — procesando y guardando");
        String result = "CACHE_RESULT[" + data + "_cached]";
        cache.put(data, result);
        return result;
    }

    @Override
    public String getStrategyName() {
        return "CacheProcessing";
    }
}
