package com.servicio1.demo.controller;

import com.servicio1.demo.dto.PosTransactionDto;
import com.servicio1.demo.model.PosTransaction;
import com.servicio1.demo.service.PosProcessingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pos")
public class PosController {

    private final PosProcessingService posProcessingService;

    public PosController(PosProcessingService posProcessingService) {
        this.posProcessingService = posProcessingService;
    }

    @PostMapping("/simulate-mq")
    public ResponseEntity<String> simulateRabbitMqMessage(@RequestBody PosTransactionDto dto) {
        posProcessingService.procesarYGuardar(dto);
        return ResponseEntity.ok("Mensaje procesado y almacenado correctamente en la BD simulada (Singleton).");
    }

    @GetMapping("/data")
    public ResponseEntity<List<PosTransaction>> getPosData() {
        return ResponseEntity.ok(posProcessingService.obtenerTodasLasTransacciones());
    }
}
