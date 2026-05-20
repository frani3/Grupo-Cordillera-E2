package com.servicio1.demo.dto;

import lombok.Data;
import java.util.List;

@Data
public class PosTransactionDto {
    private String trx_id;
    private String sucursal;
    private String caja_id;
    private String fecha_hora;
    private Double monto_total;
    private String metodo_pago;
    private String vendedor_id;
    private List<PosProductDto> productos;
}
