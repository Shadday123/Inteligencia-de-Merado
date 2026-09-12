package com.relacionamiento.alertas.domain;

public enum Herramienta {
    CONTRATACION_PUBLICA("Prototipo: Contratación Pública v1"),
    EMPRESAS_OBJETIVO("Prototipo: Monitoreo de Empresas Objetivo v1"),
    COOPERACION_INTERNACIONAL("Prototipo: Cooperación Internacional v1");

    private final String descripcion;

    Herramienta(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
