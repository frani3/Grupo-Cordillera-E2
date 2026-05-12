package com.grupocordillera.orqservice.strategy;

import com.grupocordillera.orqservice.model.KpiResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Qualifier("realTime")
public class RealTimeKpiStrategy implements KpiStrategy {

    @Override
    public KpiResponse obtenerDatos(String kpiId) {
        return KpiResponse.builder()
                .kpiId(kpiId)
                .nombre("Ventas Mensuales")
                .valor(98750.50)
                .staleData(false)
                .build();
    }
}
