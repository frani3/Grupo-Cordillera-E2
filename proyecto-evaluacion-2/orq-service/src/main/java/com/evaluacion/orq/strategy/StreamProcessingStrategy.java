package com.evaluacion.orq.strategy;

// PATRÓN STRATEGY: Justificación técnica para evaluación parcial 2
// ConcreteStrategy B: procesa datos registro a registro en tiempo real.
public class StreamProcessingStrategy implements IProcessingStrategy {

    @Override
    public String process(String data) {
        System.out.println("[StreamStrategy] Procesando en tiempo real (streaming)");
        return "STREAM_RESULT[" + data + "_realtime]";
    }

    @Override
    public String getStrategyName() {
        return "StreamProcessing";
    }
}
