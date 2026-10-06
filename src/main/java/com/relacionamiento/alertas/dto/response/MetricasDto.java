package com.relacionamiento.alertas.dto.response;

import java.util.Map;

public class MetricasDto {
    private long totalAlertas;
    private long totalNuevas;
    private long totalRevisadas;
    private long totalDescartadas;
    private Map<String, Long> porHerramienta;
    private Map<String, Long> porRelevancia;

    public MetricasDto() {}

    public MetricasDto(long totalAlertas, long totalNuevas, long totalRevisadas, long totalDescartadas,
                       Map<String, Long> porHerramienta, Map<String, Long> porRelevancia) {
        this.totalAlertas = totalAlertas;
        this.totalNuevas = totalNuevas;
        this.totalRevisadas = totalRevisadas;
        this.totalDescartadas = totalDescartadas;
        this.porHerramienta = porHerramienta;
        this.porRelevancia = porRelevancia;
    }

    public long getTotalAlertas() { return totalAlertas; }
    public void setTotalAlertas(long totalAlertas) { this.totalAlertas = totalAlertas; }

    public long getTotalNuevas() { return totalNuevas; }
    public void setTotalNuevas(long totalNuevas) { this.totalNuevas = totalNuevas; }

    public long getTotalRevisadas() { return totalRevisadas; }
    public void setTotalRevisadas(long totalRevisadas) { this.totalRevisadas = totalRevisadas; }

    public long getTotalDescartadas() { return totalDescartadas; }
    public void setTotalDescartadas(long totalDescartadas) { this.totalDescartadas = totalDescartadas; }

    public Map<String, Long> getPorHerramienta() { return porHerramienta; }
    public void setPorHerramienta(Map<String, Long> porHerramienta) { this.porHerramienta = porHerramienta; }

    public Map<String, Long> getPorRelevancia() { return porRelevancia; }
    public void setPorRelevancia(Map<String, Long> porRelevancia) { this.porRelevancia = porRelevancia; }
}
