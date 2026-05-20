package com.servicio1.demo.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "pos_transaction")
public class PosTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String transactionId;
    private String sucursal;
    private String cajaId;
    
    // Fecha limpia en formato YYYY-MM-DD
    private LocalDate fecha;
    
    private Double montoTotal;
    private String metodoPago;
    private String vendedorId;
    
    private String status; // EJ: PROCESADO, ERROR, ORQUESTADOR_FALLA
    
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "transaction")
    private List<PosTransactionItem> items;
}
