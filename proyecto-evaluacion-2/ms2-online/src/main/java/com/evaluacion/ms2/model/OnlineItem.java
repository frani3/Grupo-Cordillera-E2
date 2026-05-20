package com.evaluacion.ms2.model;

import lombok.Data;

@Data
public class OnlineItem {
    private String sku;
    private Integer cantidad;
    private Double precioUnitario;
}
