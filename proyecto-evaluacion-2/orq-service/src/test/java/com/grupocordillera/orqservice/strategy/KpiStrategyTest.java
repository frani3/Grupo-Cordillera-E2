package com.grupocordillera.orqservice.strategy;

import com.grupocordillera.orqservice.model.KpiResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KpiStrategyTest {

    @Test
    void realTimeStrategy_debeRetornarDatosFrescos() {
        RealTimeKpiStrategy strategy = new RealTimeKpiStrategy();

        KpiResponse response = strategy.obtenerDatos("kpi-001");

        assertNotNull(response);
        assertEquals("kpi-001", response.getKpiId());
        assertFalse(response.isStaleData(), "RealTime debe retornar staleData=false");
        assertNotNull(response.getValor());
        assertNotNull(response.getNombre());
    }

    @Test
    void fallbackStrategy_debeRetornarDatosDeCache() {
        FallbackKpiStrategy strategy = new FallbackKpiStrategy();

        KpiResponse response = strategy.obtenerDatos("kpi-001");

        assertNotNull(response);
        assertEquals("kpi-001", response.getKpiId());
        assertTrue(response.isStaleData(), "Fallback HU-08 debe retornar staleData=true");
        assertNotNull(response.getValor());
        assertNotNull(response.getNombre());
    }

    @Test
    void realTimeStrategy_conDistintoKpiId_propagaElId() {
        RealTimeKpiStrategy strategy = new RealTimeKpiStrategy();

        KpiResponse response = strategy.obtenerDatos("ventas-q4");

        assertEquals("ventas-q4", response.getKpiId());
    }

    @Test
    void fallbackStrategy_conDistintoKpiId_propagaElId() {
        FallbackKpiStrategy strategy = new FallbackKpiStrategy();

        KpiResponse response = strategy.obtenerDatos("margen-neto");

        assertEquals("margen-neto", response.getKpiId());
    }
}
