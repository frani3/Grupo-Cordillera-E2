package com.evaluacion.orq.strategy;

import java.util.List;
import java.util.Map;

// PATRÓN STRATEGY: Justificación técnica para evaluación parcial 2
// Interfaz común para todos los algoritmos de procesamiento.
// Recibe la lista de transacciones reales del data-ms y retorna
// un resumen procesado según el algoritmo concreto elegido.
public interface ProcessingStrategy {
    String process(String requestId, List<Map<String, Object>> transactions);
    String getName();
}
