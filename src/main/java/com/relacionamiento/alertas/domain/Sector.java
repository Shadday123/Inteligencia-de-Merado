package com.relacionamiento.alertas.domain;

public enum Sector {
    TECNOLOGIA("Tecnología e Innovación"),
    EDUCACION("Educación y Formación"),
    SALUD("Salud y Protección Social"),
    INFRAESTRUCTURA("Infraestructura y Transporte"),
    DEFENSA("Defensa y Seguridad");

    private final String descripcion;

    Sector(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
