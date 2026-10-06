package com.relacionamiento.alertas.dto.request;

import com.relacionamiento.alertas.domain.EstadoAlerta;
import jakarta.validation.constraints.NotNull;

public class EstadoUpdateDto {

    @NotNull(message = "El estado es obligatorio (NUEVA, REVISADA, DESCARTADA)")
    private EstadoAlerta estado;

    public EstadoUpdateDto() {}

    public EstadoUpdateDto(EstadoAlerta estado) {
        this.estado = estado;
    }

    public EstadoAlerta getEstado() { return estado; }
    public void setEstado(EstadoAlerta estado) { this.estado = estado; }
}
