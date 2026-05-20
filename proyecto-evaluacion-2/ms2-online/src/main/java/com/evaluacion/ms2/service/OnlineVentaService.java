package com.evaluacion.ms2.service;

import com.evaluacion.ms2.dto.OnlineProductoDto;
import com.evaluacion.ms2.dto.OnlineVentaDto;
import com.evaluacion.ms2.model.OnlineItem;
import com.evaluacion.ms2.model.OnlineVenta;
import com.evaluacion.ms2.repository.OnlineVentaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class OnlineVentaService {

    private final OnlineVentaRepository repository;

    public OnlineVentaService(OnlineVentaRepository repository) {
        this.repository = repository;
    }

    // Misma logica de limpieza que PosProcessingService del MS1
    public void procesarYGuardar(OnlineVentaDto dto) {
        System.out.println("MS2 procesando venta online: " + dto.getTrx_id());

        // 1. Validar campos requeridos (igual que MS1)
        if (dto.getTrx_id() == null || dto.getMonto_total() == null) {
            System.err.println("MS2 datos incompletos descartados: falta trx_id o monto_total");
            return;
        }

        // 2. Parsear y limpiar fecha (igual que MS1)
        LocalDate fechaLimpia;
        try {
            fechaLimpia = LocalDate.parse(
                dto.getFecha_hora().split("T")[0],
                DateTimeFormatter.ISO_LOCAL_DATE
            );
        } catch (Exception e) {
            System.err.println("MS2 fecha invalida, usando fecha actual: " + e.getMessage());
            fechaLimpia = LocalDate.now();
        }

        // 3. Construir modelo limpio
        OnlineVenta venta = new OnlineVenta();
        venta.setTransactionId(dto.getTrx_id());
        venta.setCanal(dto.getCanal() != null ? dto.getCanal() : "online");
        venta.setPlataforma(dto.getPlataforma());
        venta.setEmailCliente(dto.getEmail_cliente());
        venta.setDireccionEnvio(dto.getDireccion_envio());
        venta.setFecha(fechaLimpia);
        venta.setMontoTotal(dto.getMonto_total());
        venta.setMetodoPago(dto.getMetodo_pago());
        venta.setStatus("PROCESADO_OK");
        venta.setCreatedAt(LocalDateTime.now());

        // 4. Asociar items
        List<OnlineItem> items = new ArrayList<>();
        if (dto.getProductos() != null) {
            for (OnlineProductoDto p : dto.getProductos()) {
                OnlineItem item = new OnlineItem();
                item.setSku(p.getSku());
                item.setCantidad(p.getCantidad());
                item.setPrecioUnitario(p.getPrecio_unitario());
                items.add(item);
            }
        }
        venta.setItems(items);

        repository.save(venta);
        System.out.println("MS2 venta guardada: " + venta.getTransactionId() + " plataforma=" + venta.getPlataforma());
    }

    public List<OnlineVenta> obtenerTodas() {
        return repository.findAll();
    }

    public List<OnlineVenta> obtenerUltimosDias(int dias) {
        return repository.findUltimosDias(dias);
    }
}
