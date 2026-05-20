package com.evaluacion.ms2.dto;

import lombok.Data;
import java.util.List;

@Data
public class OnlineVentaDto {
    // Campos comunes con MS1
    private String trx_id;
    private String fecha_hora;
    private Double monto_total;
    private String metodo_pago;
    private List<OnlineProductoDto> productos;

    // Campos exclusivos de ventas online
    private String canal;            // "online" siempre
    private String plataforma;       // "web" | "app" | "marketplace"
    private String email_cliente;
    private String direccion_envio;
}
