package com.grupocordillera.orqservice.controller;

import com.grupocordillera.orqservice.model.KpiResponse;
import com.grupocordillera.orqservice.service.KpiOrchestratorContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kpis")
public class KpiController {

    private final KpiOrchestratorContext orchestrator;

    public KpiController(KpiOrchestratorContext orchestrator) {
        this.orchestrator = orchestrator;
    }

    @GetMapping("/{kpiId}")
    public ResponseEntity<KpiResponse> getKpi(@PathVariable String kpiId) {
        // TODO: reemplazar por cb.getState() == CircuitBreaker.State.CLOSED
        // cuando se integre Resilience4j con el data-ms del compañero B.
        boolean isServiceUp = true;

        KpiResponse response = orchestrator.orquestarKpi(kpiId, isServiceUp);
        return ResponseEntity.ok(response);
    }
}
