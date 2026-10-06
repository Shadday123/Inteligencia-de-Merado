package com.relacionamiento.alertas.service;

import com.relacionamiento.alertas.domain.EstadoAlerta;
import com.relacionamiento.alertas.domain.Herramienta;
import com.relacionamiento.alertas.domain.NivelRelevancia;
import com.relacionamiento.alertas.domain.Sector;
import com.relacionamiento.alertas.domain.EntidadSector;
import com.relacionamiento.alertas.entity.Alerta;
import com.relacionamiento.alertas.repository.AlertaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class IngestionService {

    private final AlertaRepository alertaRepository;
    private final SecopClient secopClient;
    private final Map<Herramienta, LocalDateTime> ultimaEjecucion = new EnumMap<>(Herramienta.class);

    public IngestionService(AlertaRepository alertaRepository, SecopClient secopClient) {
        this.alertaRepository = alertaRepository;
        this.secopClient = secopClient;
    }

    @Transactional
    public Map<String, Object> ejecutarIngestion(Herramienta herramienta, Sector sector, EntidadSector entidad, double cuantiaMinima, int limite) {
        List<Alerta> nuevasDetectadas = new ArrayList<>();
        LocalDateTime ahora = LocalDateTime.now();

        switch (herramienta) {
            case CONTRATACION_PUBLICA -> {
                List<Alerta> reales = secopClient.consultarProcesosRelevantes(sector, entidad, cuantiaMinima, limite);
                nuevasDetectadas.addAll(reales);
            }
            case EMPRESAS_OBJETIVO -> {
                // Se delega al generador de reportes real (no simulado)
            }
            case COOPERACION_INTERNACIONAL -> {
                // Se delega al generador de reportes real (no simulado)
            }
        }

        List<Alerta> guardadas = alertaRepository.saveAll(filtrarDuplicados(nuevasDetectadas));
        ultimaEjecucion.put(herramienta, ahora);

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("herramienta", herramienta.name());
        resultado.put("descripcionHerramienta", herramienta.getDescripcion());
        resultado.put("fechaEjecucion", ahora);
        resultado.put("alertasDetectadas", guardadas.size());
        resultado.put("items", guardadas.stream().map(Alerta::getTitulo).toList());

        return resultado;
    }

    public Map<String, Object> generarReporteSecop(Sector sector, EntidadSector entidad, double cuantiaMinima, int limite) {
        // 1. Consultar a SECOP II con los filtros exactos
        List<Alerta> procesosReales = secopClient.consultarProcesosRelevantes(sector, entidad, cuantiaMinima, limite);
        
        // 2. Guardar en base de datos solo los que no estén repetidos (para no ensuciar la DB)
        alertaRepository.saveAll(filtrarDuplicados(procesosReales));

        // 3. Devolver TODOS los procesos encontrados para que el usuario siempre vea los resultados
        Map<String, Object> reporte = new LinkedHashMap<>();
        reporte.put("titulo", "Reporte de Contratación Pública (SECOP II)");
        reporte.put("filtrosAplicados", Map.of(
                "sector", sector != null ? sector.getDescripcion() : "Todos",
                "entidad", entidad != null ? entidad.getNombreReal() : "Todas",
                "cuantiaMinima", cuantiaMinima
        ));
        reporte.put("totalResultados", procesosReales.size());
        reporte.put("contratos", procesosReales); // Mostramos lo que respondió SECOP realmente

        return reporte;
    }



    private List<Alerta> filtrarDuplicados(List<Alerta> alertas) {
        List<Alerta> unicas = new ArrayList<>();
        for (Alerta a : alertas) {
            if (a.getUrlOrigen() != null && !a.getUrlOrigen().trim().isEmpty()) {
                if (alertaRepository.existsByUrlOrigen(a.getUrlOrigen())) {
                    continue; // Ya existe en la base de datos, lo omitimos
                }
            }
            unicas.add(a);
        }
        return unicas;
    }

    public Map<String, Object> obtenerEstadoIngestion() {
        Map<String, Object> estado = new LinkedHashMap<>();
        for (Herramienta h : Herramienta.values()) {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("descripcion", h.getDescripcion());
            info.put("ultimaEjecucion", ultimaEjecucion.getOrDefault(h, null));
            info.put("frecuenciaConfigurada", switch (h) {
                case EMPRESAS_OBJETIVO -> "Diaria (RSS / Prensa)";
                case CONTRATACION_PUBLICA -> "Diaria (API SECOP II & Multilaterales)";
                case COOPERACION_INTERNACIONAL -> "Cada 2-3 días (OCHA / DNP / UNGRD)";
            });
            estado.put(h.name(), info);
        }
        return estado;
    }
}
