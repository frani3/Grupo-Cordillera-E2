package com.servicio1.demo.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "pos_transaction_item")
public class PosTransactionItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private PosTransaction transaction;

    private String sku;
    private Integer cantidad;
    private Double precioUnitario;
}
