package com.evaluacion.orq.strategy;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProcessingContext {

    private final Map<String, ProcessingStrategy> strategies;
    private ProcessingStrategy currentStrategy;

    public ProcessingContext(List<ProcessingStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(ProcessingStrategy::getName, Function.identity()));
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

    public String executeStrategy(String requestId, List<Map<String, Object>> transactions) {
        return currentStrategy.process(requestId, transactions);
    }

    public String getCurrentStrategyName() {
        return currentStrategy.getName();
    }
}
