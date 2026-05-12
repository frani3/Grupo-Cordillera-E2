package com.grupocordillera.orqservice.strategy;

import com.grupocordillera.orqservice.model.KpiResponse;

public interface KpiStrategy {

    KpiResponse obtenerDatos(String kpiId);
}
