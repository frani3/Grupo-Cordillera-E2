package com.evaluacion.orq.strategy;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// PATRÓN STRATEGY: Justificación técnica para evaluación parcial 2
// Context que mantiene la referencia a la estrategia activa.
// Permite cambiar el algoritmo en runtime sin tocar el cliente.
@Service
public class ProcessingContext {

    private final Map<String, ProcessingStrategy> strategies;
    private ProcessingStrategy currentStrategy;

    public ProcessingContext(List<ProcessingStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(ProcessingStrategy::getName, Function.identity()));
        // Estrategia por defecto
        this.currentStrategy = strategies.get("batch");
    }

    public void setStrategy(String strategyName) {
        ProcessingStrategy strategy = strategies.get(strategyName);
        if (strategy == null) {
            throw new IllegalArgumentException(
                    "Estrategia desconocida: '" + strategyName + "'. Disponibles: " + strategies.keySet());
        }
        this.currentStrategy = strategy;
    }

    public String executeStrategy(String requestId, Object rawData) {
        return currentStrategy.process(requestId, rawData);
    }

    public String getCurrentStrategyName() {
        return currentStrategy.getName();
    }
}
