package com.evaluacion.orq.strategy;

// PATRÓN STRATEGY: Justificación técnica para evaluación parcial 2
// Subject (interfaz): contrato que todas las estrategias deben cumplir.
// ProcessingContext depende de esta abstracción → principio DIP.
public interface IProcessingStrategy {
    String process(String data);
    String getStrategyName();
}
