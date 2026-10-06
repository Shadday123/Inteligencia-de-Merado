package com.relacionamiento.alertas.dto.response;

import com.relacionamiento.alertas.domain.EstadoAlerta;
import com.relacionamiento.alertas.domain.Herramienta;
import com.relacionamiento.alertas.domain.NivelRelevancia;
import com.relacionamiento.alertas.entity.Alerta;

import java.time.LocalDateTime;

public class AlertaResponseDto {
    private Long id;
    private Herramienta herramienta;
    private String titulo;
    private String descripcion;
    private String fuente;
    private String tipoInformacion;
    private String empresaEntidadRelacionada;
    private String urlOrigen;
    private String palabrasClaveDetectadas;
    private NivelRelevancia nivelRelevancia;
    private EstadoAlerta estadoAlerta;
    private LocalDateTime fechaPublicacion;
    private LocalDateTime fechaDeteccion;

    public AlertaResponseDto() {}

    public static AlertaResponseDto fromEntity(Alerta entity) {
        AlertaResponseDto dto = new AlertaResponseDto();
        dto.setId(entity.getId());
        dto.setHerramienta(entity.getHerramienta());
        dto.setTitulo(entity.getTitulo());
        dto.setDescripcion(entity.getDescripcion());
        dto.setFuente(entity.getFuente());
        dto.setTipoInformacion(entity.getTipoInformacion());
        dto.setEmpresaEntidadRelacionada(entity.getEmpresaEntidadRelacionada());
        dto.setUrlOrigen(entity.getUrlOrigen());
        dto.setPalabrasClaveDetectadas(entity.getPalabrasClaveDetectadas());
        dto.setNivelRelevancia(entity.getNivelRelevancia());
        dto.setEstadoAlerta(entity.getEstadoAlerta());
        dto.setFechaPublicacion(entity.getFechaPublicacion());
        dto.setFechaDeteccion(entity.getFechaDeteccion());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Herramienta getHerramienta() { return herramienta; }
    public void setHerramienta(Herramienta herramienta) { this.herramienta = herramienta; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getFuente() { return fuente; }
    public void setFuente(String fuente) { this.fuente = fuente; }

    public String getTipoInformacion() { return tipoInformacion; }
    public void setTipoInformacion(String tipoInformacion) { this.tipoInformacion = tipoInformacion; }

    public String getEmpresaEntidadRelacionada() { return empresaEntidadRelacionada; }
    public void setEmpresaEntidadRelacionada(String empresaEntidadRelacionada) { this.empresaEntidadRelacionada = empresaEntidadRelacionada; }

    public String getUrlOrigen() { return urlOrigen; }
    public void setUrlOrigen(String urlOrigen) { this.urlOrigen = urlOrigen; }

    public String getPalabrasClaveDetectadas() { return palabrasClaveDetectadas; }
    public void setPalabrasClaveDetectadas(String palabrasClaveDetectadas) { this.palabrasClaveDetectadas = palabrasClaveDetectadas; }

    public NivelRelevancia getNivelRelevancia() { return nivelRelevancia; }
    public void setNivelRelevancia(NivelRelevancia nivelRelevancia) { this.nivelRelevancia = nivelRelevancia; }

    public EstadoAlerta getEstadoAlerta() { return estadoAlerta; }
    public void setEstadoAlerta(EstadoAlerta estadoAlerta) { this.estadoAlerta = estadoAlerta; }

    public LocalDateTime getFechaPublicacion() { return fechaPublicacion; }
    public void setFechaPublicacion(LocalDateTime fechaPublicacion) { this.fechaPublicacion = fechaPublicacion; }

    public LocalDateTime getFechaDeteccion() { return fechaDeteccion; }
    public void setFechaDeteccion(LocalDateTime fechaDeteccion) { this.fechaDeteccion = fechaDeteccion; }
}
