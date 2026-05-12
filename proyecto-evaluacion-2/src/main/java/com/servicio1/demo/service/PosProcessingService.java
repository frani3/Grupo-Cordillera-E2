package com.servicio1.demo.service;

import com.servicio1.demo.dto.PosProductDto;
import com.servicio1.demo.dto.PosTransactionDto;
import com.servicio1.demo.model.PosTransaction;
import com.servicio1.demo.model.PosTransactionItem;
import com.servicio1.demo.repository.PosTransactionRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class PosProcessingService {

    private final PosTransactionRepository repository;

    public PosProcessingService(PosTransactionRepository repository) {
        this.repository = repository;
    }

    public void procesarYGuardar(PosTransactionDto dto) {
        System.out.println("Iniciando procesamiento transaccion (Simulando Ingesta MQ): " + dto.getTrx_id());

        // 1. Limpieza y validacion de datos
        if (dto.getTrx_id() == null || dto.getMonto_total() == null) {
            System.err.println("Datos incompletos y descartados: Faltan ID o monto_total.");
            return;
        }

        // Parsear fecha y asegurar que el formato sea YYYY-MM-DD
        LocalDate cleanDate;
        try {
            cleanDate = LocalDate.parse(dto.getFecha_hora().split("T")[0], DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            System.err.println("Falla formato fecha. Guardando error. " + e.getMessage());
            cleanDate = LocalDate.now();
        }

        PosTransaction trxEnDb = new PosTransaction();
        trxEnDb.setTransactionId(dto.getTrx_id());
        trxEnDb.setSucursal(dto.getSucursal());
        trxEnDb.setCajaId(dto.getCaja_id());
        trxEnDb.setFecha(cleanDate);
        trxEnDb.setMontoTotal(dto.getMonto_total());
        trxEnDb.setMetodoPago(dto.getMetodo_pago());
        trxEnDb.setVendedorId(dto.getVendedor_id());

        // 2. Asociar Detalle (Items)
        List<PosTransactionItem> items = new ArrayList<>();
        if (dto.getProductos() != null) {
            for (PosProductDto prodDto : dto.getProductos()) {
                PosTransactionItem item = new PosTransactionItem();
                item.setSku(prodDto.getSku());
                item.setCantidad(prodDto.getCantidad());
                item.setPrecioUnitario(prodDto.getPrecio_unitario());
                item.setTransaction(trxEnDb);
                items.add(item);
            }
        }
        trxEnDb.setItems(items);

        // 3. Guardo estado en BD (Simulada por Singleton)
        trxEnDb.setStatus("PROCESADO_OK_ESPERANDO_ORQUESTADOR");
        repository.save(trxEnDb);
        System.out.println("Transaccion guardada exitosamente. Lista para ser solicitada por el Orquestador.");
    }
    
    public List<PosTransaction> obtenerTodasLasTransacciones() {
        return repository.findAll();
    }
}
