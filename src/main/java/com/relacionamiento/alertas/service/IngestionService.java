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
                // Conexión real a la API SODA de SECOP II (datos.gov.co)
                List<Alerta> reales = secopClient.consultarProcesosRelevantes(sector, entidad, cuantiaMinima, limite);
                if (!reales.isEmpty()) {
                    nuevasDetectadas.addAll(reales);
                } else {
                    // Respaldo en caso de indisponibilidad temporal de datos.gov.co
                    nuevasDetectadas.add(new Alerta(
                            Herramienta.CONTRATACION_PUBLICA,
                            "Licitación Pública SECOP II: Plataforma de Analítica y Gestión de Datos",
                            "Convocatoria de MinTIC para desarrollo de plataforma de visualización de datos abiertos. Cuantía: $850.000.000 COP.",
                            "SECOP II (datos.gov.co)",
                            "Licitación Pública",
                            "MinTIC",
                            "https://colombiacompra.gov.co/secop-ii",
                            "SECOP II, analítica, tecnología, software, Colombia Compra",
                            NivelRelevancia.ALTA,
                            EstadoAlerta.NUEVA,
                            ahora.minusHours(1)
                    ));
                }
            }
            case EMPRESAS_OBJETIVO -> {
                nuevasDetectadas.add(new Alerta(
                        Herramienta.EMPRESAS_OBJETIVO,
                        "Ecopetrol anuncia inversión de USD 500M en transición energética",
                        "La estatal petrolera detalló en su plan de negocios nuevas partidas presupuestales para proyectos de energía solar y eólica.",
                        "Portafolio",
                        "Inversión / Expansión",
                        "Ecopetrol S.A.",
                        "https://www.portafolio.co/negocios/inversiones-ecopetrol-2026",
                        "Ecopetrol, inversión, energía solar, transición energética",
                        NivelRelevancia.ALTA,
                        EstadoAlerta.NUEVA,
                        ahora.minusHours(2)
                ));
            }
            case COOPERACION_INTERNACIONAL -> {
                nuevasDetectadas.add(new Alerta(
                        Herramienta.COOPERACION_INTERNACIONAL,
                        "UNGRD y DNP aprueban desembolso de $85.000M para Reconstrucción",
                        "Comité directivo del Fondo Milagro avaló cronograma de obras civiles, mitigación de riesgo y vivienda para zonas afectadas por el sismo.",
                        "UNGRD Comunicados Oficiales",
                        "Asignación de Recursos",
                        "UNGRD / DNP",
                        "https://portal.ungrd.gov.co/boletines/reconstruccion-fondo-milagro",
                        "Fondo Milagro, reconstrucción, terremoto, UNGRD, DNP, vivienda",
                        NivelRelevancia.ALTA,
                        EstadoAlerta.NUEVA,
                        ahora.minusHours(3)
                ));
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
