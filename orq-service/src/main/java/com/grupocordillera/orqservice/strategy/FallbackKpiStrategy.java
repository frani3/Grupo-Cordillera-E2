package com.grupocordillera.orqservice.strategy;

import com.grupocordillera.orqservice.model.KpiResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Qualifier("fallback")
public class FallbackKpiStrategy implements KpiStrategy {

    // HU-08: cuando el Circuit Breaker está abierto, los datos se recuperan
    // desde Redis (caché). staleData = true señaliza al consumidor que el
    // valor puede no reflejar el estado más reciente del sistema.
    @Override
    public KpiResponse obtenerDatos(String kpiId) {
        return KpiResponse.builder()
                .kpiId(kpiId)
                .nombre("Ventas Mensuales (caché Redis)")
                .valor(95000.00)
                .staleData(true)
                .build();
    }
}
