package com.evaluacion.ms2.dto;

import lombok.Data;

@Data
public class OnlineProductoDto {
    private String sku;
    private Integer cantidad;
    private Double precio_unitario;
}
