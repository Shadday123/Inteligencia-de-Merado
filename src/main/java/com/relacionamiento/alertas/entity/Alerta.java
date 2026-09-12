package com.relacionamiento.alertas.entity;

import com.relacionamiento.alertas.domain.EstadoAlerta;
import com.relacionamiento.alertas.domain.Herramienta;
import com.relacionamiento.alertas.domain.NivelRelevancia;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alertas")
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Herramienta herramienta;

    @Column(nullable = false, length = 300)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false, length = 100)
    private String fuente;

    @Column(name = "tipo_informacion", length = 100)
    private String tipoInformacion;

    @Column(name = "empresa_entidad_relacionada", length = 255)
    private String empresaEntidadRelacionada;

    @Column(name = "url_origen", length = 500)
    private String urlOrigen;

    @Column(name = "palabras_clave_detectadas", length = 500)
    private String palabrasClaveDetectadas;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_relevancia", nullable = false, length = 20)
    private NivelRelevancia nivelRelevancia;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_alerta", nullable = false, length = 20)
    private EstadoAlerta estadoAlerta;

    @Column(name = "fecha_publicacion")
    private LocalDateTime fechaPublicacion;

    @Column(name = "fecha_deteccion", nullable = false)
    private LocalDateTime fechaDeteccion;

    @PrePersist
    public void prePersist() {
        if (this.fechaDeteccion == null) {
            this.fechaDeteccion = LocalDateTime.now();
        }
        if (this.estadoAlerta == null) {
            this.estadoAlerta = EstadoAlerta.NUEVA;
        }
    }

    public Alerta() {}

    public Alerta(Herramienta herramienta, String titulo, String descripcion, String fuente,
                  String tipoInformacion, String empresaEntidadRelacionada, String urlOrigen,
                  String palabrasClaveDetectadas, NivelRelevancia nivelRelevancia,
                  EstadoAlerta estadoAlerta, LocalDateTime fechaPublicacion) {
        this.herramienta = herramienta;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.fuente = fuente;
        this.tipoInformacion = tipoInformacion;
        this.empresaEntidadRelacionada = empresaEntidadRelacionada;
        this.urlOrigen = urlOrigen;
        this.palabrasClaveDetectadas = palabrasClaveDetectadas;
        this.nivelRelevancia = nivelRelevancia;
        this.estadoAlerta = estadoAlerta != null ? estadoAlerta : EstadoAlerta.NUEVA;
        this.fechaPublicacion = fechaPublicacion;
        this.fechaDeteccion = LocalDateTime.now();
    }

    // Getters and Setters
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
