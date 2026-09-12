package com.relacionamiento.alertas.dto.request;

import com.relacionamiento.alertas.domain.Herramienta;
import com.relacionamiento.alertas.domain.NivelRelevancia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public class AlertaCreateDto {

    @NotNull(message = "La herramienta es obligatoria (EMPRESAS, SECOP_COOPERACION, FONDO_RECUPERACION)")
    private Herramienta herramienta;

    @NotBlank(message = "El título de la alerta es obligatorio")
    @Size(max = 300, message = "El título no puede superar los 300 caracteres")
    private String titulo;

    @NotBlank(message = "La descripción o resumen es obligatorio")
    private String descripcion;

    @NotBlank(message = "La fuente es obligatoria (ej: SECOP II, Portafolio, UNGRD, etc.)")
    @Size(max = 100, message = "La fuente no puede superar los 100 caracteres")
    private String fuente;

    private String tipoInformacion;
    private String empresaEntidadRelacionada;
    private String urlOrigen;
    private String palabrasClaveDetectadas;

    @NotNull(message = "El nivel de relevancia es obligatorio (ALTA, MEDIA, BAJA)")
    private NivelRelevancia nivelRelevancia;

    private LocalDateTime fechaPublicacion;

    public AlertaCreateDto() {}

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

    public LocalDateTime getFechaPublicacion() { return fechaPublicacion; }
    public void setFechaPublicacion(LocalDateTime fechaPublicacion) { this.fechaPublicacion = fechaPublicacion; }
}
