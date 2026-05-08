package com.evaluacion.orq.strategy;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

// Tests unitarios puros — Patrón Strategy (orq-service)
// Cobertura: estrategias concretas, contexto, cambio en runtime
@DisplayName("Patrón Strategy — Orq Service")
class ProcessingContextTest {

    private ProcessingContext context;

    @BeforeEach
    void setUp() { context = new ProcessingContext(); }

    @Test @DisplayName("BatchStrategy produce resultado con prefijo BATCH_RESULT")
    void batch_producesCorrectResult() {
        context.setStrategy(new BatchProcessingStrategy(50));
        String result = context.executeStrategy("datos");
        assertTrue(result.startsWith("BATCH_RESULT"));
        assertTrue(result.contains("DATOS")); // toUpperCase aplicado
    }

    @Test @DisplayName("StreamStrategy produce resultado con sufijo _realtime")
    void stream_producesCorrectResult() {
        context.setStrategy(new StreamProcessingStrategy());
        String result = context.executeStrategy("sensor");
        assertTrue(result.startsWith("STREAM_RESULT"));
        assertTrue(result.contains("realtime"));
    }

    @Test @DisplayName("CacheStrategy retorna mismo resultado en segunda llamada (cache HIT)")
    void cache_returnsSameResultOnSecondCall() {
        CacheProcessingStrategy strategy = new CacheProcessingStrategy();
        context.setStrategy(strategy);

        String first  = context.executeStrategy("clave-A");
        String second = context.executeStrategy("clave-A");
        assertEquals(first, second, "Cache HIT debe retornar el mismo resultado");
    }

    @Test @DisplayName("CacheStrategy procesa claves distintas de forma independiente")
    void cache_processesDifferentKeysIndependently() {
        context.setStrategy(new CacheProcessingStrategy());
        assertNotEquals(context.executeStrategy("A"), context.executeStrategy("B"));
    }

    @Test @DisplayName("Context permite cambiar estrategia en runtime")
    void context_allowsRuntimeStrategyChange() {
        context.setStrategy(new BatchProcessingStrategy(10));
        String batchResult = context.executeStrategy("data");

        context.setStrategy(new StreamProcessingStrategy());
        String streamResult = context.executeStrategy("data");

        assertNotEquals(batchResult, streamResult, "La estrategia debe cambiar el resultado");
    }

    @Test @DisplayName("getCurrentStrategyName() reporta la estrategia activa")
    void context_reportsCurrentStrategyName() {
        context.setStrategy(new StreamProcessingStrategy());
        assertEquals("StreamProcessing", context.getCurrentStrategyName());
    }

    @Test @DisplayName("BatchStrategy incluye el tamaño del lote en su nombre")
    void batch_includesBatchSizeInName() {
        assertTrue(new BatchProcessingStrategy(200).getStrategyName().contains("200"));
    }
}
