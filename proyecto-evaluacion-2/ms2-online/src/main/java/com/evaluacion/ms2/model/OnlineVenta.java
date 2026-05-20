package com.evaluacion.ms2.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OnlineVenta {
    private Long id;
    private String transactionId;
    private String canal;
    private String plataforma;
    private String emailCliente;
    private String direccionEnvio;
    private LocalDate fecha;
    private Double montoTotal;
    private String metodoPago;
    private String status;
    private LocalDateTime createdAt;

    @JsonIgnore
    private List<OnlineItem> items;
}
