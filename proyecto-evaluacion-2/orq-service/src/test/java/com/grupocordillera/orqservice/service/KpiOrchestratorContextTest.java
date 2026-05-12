package com.grupocordillera.orqservice.service;

import com.grupocordillera.orqservice.model.KpiResponse;
import com.grupocordillera.orqservice.strategy.KpiStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KpiOrchestratorContextTest {

    @Mock
    private KpiStrategy realTimeStrategy;

    @Mock
    private KpiStrategy fallbackStrategy;

    private KpiOrchestratorContext context;

    @BeforeEach
    void setUp() {
        context = new KpiOrchestratorContext(realTimeStrategy, fallbackStrategy);
    }

    @Test
    void cuandoServicioActivo_debeUsarRealTimeStrategy() {
        KpiResponse expected = KpiResponse.builder()
                .kpiId("kpi-1")
                .nombre("Ventas")
                .valor(1000.0)
                .staleData(false)
                .build();
        when(realTimeStrategy.obtenerDatos("kpi-1")).thenReturn(expected);

        KpiResponse result = context.orquestarKpi("kpi-1", true);

        assertEquals(expected, result);
        assertFalse(result.isStaleData());
        verify(realTimeStrategy, times(1)).obtenerDatos("kpi-1");
        verify(fallbackStrategy, never()).obtenerDatos(any());
    }

    @Test
    void cuandoCircuitBreakerAbierto_debeUsarFallbackStrategy() {
        KpiResponse expected = KpiResponse.builder()
                .kpiId("kpi-1")
                .nombre("Ventas (caché)")
                .valor(950.0)
                .staleData(true)
                .build();
        when(fallbackStrategy.obtenerDatos("kpi-1")).thenReturn(expected);

        KpiResponse result = context.orquestarKpi("kpi-1", false);

        assertEquals(expected, result);
        assertTrue(result.isStaleData(), "HU-08: datos de Redis deben indicar staleData=true");
        verify(fallbackStrategy, times(1)).obtenerDatos("kpi-1");
        verify(realTimeStrategy, never()).obtenerDatos(any());
    }

    @Test
    void resultadoDelContexto_refleja_laRespuestaDeLaEstrategia() {
        KpiResponse fallbackResponse = KpiResponse.builder()
                .kpiId("kpi-margen")
                .valor(88.5)
                .staleData(true)
                .build();
        when(fallbackStrategy.obtenerDatos("kpi-margen")).thenReturn(fallbackResponse);

        KpiResponse result = context.orquestarKpi("kpi-margen", false);

        assertEquals(88.5, result.getValor());
        assertEquals("kpi-margen", result.getKpiId());
    }
}
