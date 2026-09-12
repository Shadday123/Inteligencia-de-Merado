package com.relacionamiento.alertas.domain;

public enum Herramienta {
    EMPRESAS("Herramienta 1: Monitoreo de Empresas Objetivo"),
    SECOP_COOPERACION("Herramienta 2: SECOP II y Cooperación Internacional"),
    FONDO_RECUPERACION("Herramienta 3: Fondo de Recuperación Post-Terremoto");

    private final String descripcion;

    Herramienta(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
