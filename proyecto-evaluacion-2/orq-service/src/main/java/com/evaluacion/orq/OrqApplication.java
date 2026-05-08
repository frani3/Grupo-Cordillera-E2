package com.evaluacion.orq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// PATRÓN STRATEGY: Justificación técnica para evaluación parcial 2
// =============================================================
// Problema: El orquestador procesa solicitudes con distintos
// algoritmos (batch, streaming, caché) que deben poder cambiar
// en runtime sin modificar el contexto ni los otros algoritmos.
//
// Solución: Encapsular cada algoritmo en una clase que implemente
// IProcessingStrategy. El contexto delega la ejecución a la
// estrategia configurada dinámicamente.
//
// Participantes:
//   Strategy (interfaz)  → IProcessingStrategy
//   ConcreteStrategy A   → BatchProcessingStrategy
//   ConcreteStrategy B   → StreamProcessingStrategy
//   ConcreteStrategy C   → CacheProcessingStrategy
//   Context              → ProcessingContext
// =============================================================
@SpringBootApplication
public class OrqApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrqApplication.class, args);
    }
}
