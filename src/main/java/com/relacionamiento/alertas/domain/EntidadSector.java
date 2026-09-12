package com.relacionamiento.alertas.domain;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum EntidadSector {
    // Tecnología
    MINTIC(Sector.TECNOLOGIA, "Ministerio de Tecnologías de la Información y las Comunicaciones"),
    AGENCIA_DIGITAL(Sector.TECNOLOGIA, "Agencia Nacional Digital"),
    COMPUTADORES_EDUCAR(Sector.TECNOLOGIA, "Computadores para Educar"),

    // Educación
    MIN_EDUCACION(Sector.EDUCACION, "Ministerio de Educación Nacional"),
    SENA(Sector.EDUCACION, "Servicio Nacional de Aprendizaje"),
    ICETEX(Sector.EDUCACION, "ICETEX"),

    // Salud
    MIN_SALUD(Sector.SALUD, "Ministerio de Salud y Protección Social"),
    INVIMA(Sector.SALUD, "INVIMA"),

    // Infraestructura
    MIN_TRANSPORTE(Sector.INFRAESTRUCTURA, "Ministerio de Transporte"),
    INVIAS(Sector.INFRAESTRUCTURA, "Instituto Nacional de Vías"),
    ANI(Sector.INFRAESTRUCTURA, "Agencia Nacional de Infraestructura"),

    // Defensa
    MIN_DEFENSA(Sector.DEFENSA, "Ministerio de Defensa Nacional"),
    POLICIA(Sector.DEFENSA, "Policía Nacional");

    private final Sector sector;
    private final String nombreReal;

    EntidadSector(Sector sector, String nombreReal) {
        this.sector = sector;
        this.nombreReal = nombreReal;
    }

    public Sector getSector() {
        return sector;
    }

    public String getNombreReal() {
        return nombreReal;
    }

    public static List<EntidadSector> obtenerPorSector(Sector sectorBuscado) {
        return Arrays.stream(values())
                .filter(entidad -> entidad.getSector() == sectorBuscado)
                .collect(Collectors.toList());
    }
}
