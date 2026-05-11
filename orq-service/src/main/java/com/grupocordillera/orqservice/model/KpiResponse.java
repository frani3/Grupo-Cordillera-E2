package com.grupocordillera.orqservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KpiResponse {

    private String kpiId;
    private String nombre;
    private Double valor;
    private boolean staleData;
}
