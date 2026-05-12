package com.grupocordillera.orqservice.model;

import java.util.Objects;

public class KpiResponse {

    private String kpiId;
    private String nombre;
    private Double valor;
    private boolean staleData;

    public KpiResponse() {}

    public KpiResponse(String kpiId, String nombre, Double valor, boolean staleData) {
        this.kpiId = kpiId;
        this.nombre = nombre;
        this.valor = valor;
        this.staleData = staleData;
    }

    public String getKpiId()        { return kpiId; }
    public String getNombre()       { return nombre; }
    public Double getValor()        { return valor; }
    public boolean isStaleData()    { return staleData; }

    public void setKpiId(String kpiId)           { this.kpiId = kpiId; }
    public void setNombre(String nombre)         { this.nombre = nombre; }
    public void setValor(Double valor)           { this.valor = valor; }
    public void setStaleData(boolean staleData)  { this.staleData = staleData; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String kpiId;
        private String nombre;
        private Double valor;
        private boolean staleData;

        public Builder kpiId(String kpiId)              { this.kpiId = kpiId;         return this; }
        public Builder nombre(String nombre)            { this.nombre = nombre;       return this; }
        public Builder valor(Double valor)              { this.valor = valor;         return this; }
        public Builder staleData(boolean staleData)     { this.staleData = staleData; return this; }

        public KpiResponse build() {
            return new KpiResponse(kpiId, nombre, valor, staleData);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof KpiResponse that)) return false;
        return staleData == that.staleData
                && Objects.equals(kpiId, that.kpiId)
                && Objects.equals(nombre, that.nombre)
                && Objects.equals(valor, that.valor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kpiId, nombre, valor, staleData);
    }
}
