package com.evaluacion.orq.strategy;

// PATRÓN STRATEGY: Justificación técnica para evaluación parcial 2
// ConcreteStrategy A: procesa datos en lotes para optimizar throughput.
// Una única razón para cambiar: modificar la lógica de procesamiento batch → SRP.
public class BatchProcessingStrategy implements IProcessingStrategy {

    private final int batchSize;

    public BatchProcessingStrategy(int batchSize) {
        this.batchSize = batchSize;
    }

    @Override
    public String process(String data) {
        System.out.println("[BatchStrategy] Procesando en lotes de " + batchSize + " registros");
        return "BATCH_RESULT[" + data.toUpperCase() + "]";
    }

    @Override
    public String getStrategyName() {
        return "BatchProcessing(size=" + batchSize + ")";
    }
}
