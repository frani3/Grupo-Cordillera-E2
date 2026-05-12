package com.grupocordillera.orqservice.service;

import com.grupocordillera.orqservice.model.KpiResponse;
import com.grupocordillera.orqservice.strategy.KpiStrategy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class KpiOrchestratorContext {

    private final KpiStrategy realTimeStrategy;
    private final KpiStrategy fallbackStrategy;

    public KpiOrchestratorContext(
            @Qualifier("realTime") KpiStrategy realTimeStrategy,
            @Qualifier("fallback") KpiStrategy fallbackStrategy) {
        this.realTimeStrategy = realTimeStrategy;
        this.fallbackStrategy = fallbackStrategy;
    }

    public KpiResponse orquestarKpi(String kpiId, boolean isServiceUp) {
        KpiStrategy estrategiaActiva = isServiceUp ? realTimeStrategy : fallbackStrategy;
        return estrategiaActiva.obtenerDatos(kpiId);
    }
}
