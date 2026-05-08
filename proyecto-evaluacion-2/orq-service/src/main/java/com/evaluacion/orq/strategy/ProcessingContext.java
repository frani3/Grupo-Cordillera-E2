package com.evaluacion.orq.strategy;

import org.springframework.stereotype.Service;

// PATRÓN STRATEGY: Justificación técnica para evaluación parcial 2
// Context: mantiene referencia a la estrategia actual y delega la ejecución.
// Permite cambiar de algoritmo en RUNTIME sin modificar este clase → OCP.
@Service
public class ProcessingContext {

    private IProcessingStrategy strategy;

    public ProcessingContext() {
        this.strategy = new BatchProcessingStrategy(100); // estrategia por defecto
    }

    public void setStrategy(IProcessingStrategy strategy) {
        System.out.println("[Context] Cambiando a estrategia: " + strategy.getStrategyName());
        this.strategy = strategy;
    }

    public String executeStrategy(String data) {
        System.out.println("[Context] Ejecutando: " + strategy.getStrategyName());
        return strategy.process(data);
    }

    public String getCurrentStrategyName() {
        return strategy.getStrategyName();
    }
}
