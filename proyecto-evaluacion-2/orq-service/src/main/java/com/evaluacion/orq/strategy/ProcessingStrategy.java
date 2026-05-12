package com.evaluacion.orq.strategy;

// PATRÓN STRATEGY: Justificación técnica para evaluación parcial 2
// Interfaz común para todos los algoritmos de procesamiento.
// Permite intercambiar estrategias en runtime sin modificar el contexto.
public interface ProcessingStrategy {
    String process(String requestId, Object rawData);
    String getName();
}
