package com.evaluacion.ms2.controller;

import com.evaluacion.ms2.dto.OnlineVentaDto;
import com.evaluacion.ms2.model.OnlineVenta;
import com.evaluacion.ms2.service.OnlineVentaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/online")
public class OnlineController {

    private final OnlineVentaService service;

    public OnlineController(OnlineVentaService service) {
        this.service = service;
    }

    @PostMapping("/venta")
    public ResponseEntity<Map<String, String>> recibirVenta(@RequestBody OnlineVentaDto dto) {
        service.procesarYGuardar(dto);
        return ResponseEntity.ok(Map.of(
                "status", "OK",
                "trx_id", dto.getTrx_id() != null ? dto.getTrx_id() : "unknown"
        ));
    }

    @GetMapping("/ventas")
    public ResponseEntity<List<OnlineVenta>> obtenerVentas() {
        return ResponseEntity.ok(service.obtenerTodas());
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        int total = service.obtenerTodas().size();
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "ms2-online",
                "totalVentas", total
        ));
    }
}
