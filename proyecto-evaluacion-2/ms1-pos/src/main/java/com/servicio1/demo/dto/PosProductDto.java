package com.servicio1.demo.dto;

import lombok.Data;

@Data
public class PosProductDto {
    private String sku;
    private Integer cantidad;
    private Double precio_unitario;
}
