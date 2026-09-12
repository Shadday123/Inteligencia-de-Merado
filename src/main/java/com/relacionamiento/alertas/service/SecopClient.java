package com.relacionamiento.alertas.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.relacionamiento.alertas.domain.EstadoAlerta;
import com.relacionamiento.alertas.domain.Herramienta;
import com.relacionamiento.alertas.domain.NivelRelevancia;
import com.relacionamiento.alertas.domain.Sector;
import com.relacionamiento.alertas.domain.EntidadSector;
import com.relacionamiento.alertas.entity.Alerta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SecopClient {

    private static final Logger log = LoggerFactory.getLogger(SecopClient.class);
    private static final String SOCRATA_SECOP_URL = "https://www.datos.gov.co/resource/p6dx-8zbt.json";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SecopClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }


    public List<Alerta> consultarProcesosRelevantes(Sector sector, EntidadSector entidad, double cuantiaMinima, int limite) {
        List<Alerta> alertas = new ArrayList<>();

        try {
            // Filtro base: Estados y Cuantía
            String whereBase = String.format(
                    "estado_del_procedimiento in ('Publicado', 'Presentación de ofertas', 'Abierto') " +
                    "AND precio_base >= %.0f", cuantiaMinima
            );

            // Armar filtro SQL dinámico
            StringBuilder filtroSql = new StringBuilder();

            // 1. Filtrar por Entidad (si viene especificada) o por Sector (si viene sector pero no entidad)
            if (entidad != null) {
                // Buscamos literalmente la entidad específica
                filtroSql.append(" AND lower(entidad) like '%")
                         .append(entidad.getNombreReal().toLowerCase().replace("ministerio de ", "").replace(" nacional", ""))
                         .append("%'");
            } else if (sector != null) {
                // Buscamos todas las entidades del sector seleccionado
                List<EntidadSector> entidadesDelSector = EntidadSector.obtenerPorSector(sector);
                if (!entidadesDelSector.isEmpty()) {
                    filtroSql.append(" AND (");
                    for (int i = 0; i < entidadesDelSector.size(); i++) {
                        if (i > 0) filtroSql.append(" OR ");
                        filtroSql.append("lower(entidad) like '%")
                                 .append(entidadesDelSector.get(i).getNombreReal().toLowerCase().replace("ministerio de ", "").replace(" nacional", ""))
                                 .append("%'");
                    }
                    filtroSql.append(")");
                }
            }

            // 2. Filtrar por palabras clave de la rúbrica según el sector
            if (sector == Sector.EDUCACION) {
                filtroSql.append(" AND (lower(descripci_n_del_procedimiento) like '%software%' OR lower(descripci_n_del_procedimiento) like '%tecnolog%' OR lower(descripci_n_del_procedimiento) like '%capacitaci%')");
            } else if (sector == Sector.TECNOLOGIA) {
                filtroSql.append(" AND (lower(descripci_n_del_procedimiento) like '%software%' OR lower(descripci_n_del_procedimiento) like '%desarrollo%' OR lower(descripci_n_del_procedimiento) like '%analitica%')");
            } else {
                filtroSql.append(" AND (lower(descripci_n_del_procedimiento) like '%software%' OR lower(descripci_n_del_procedimiento) like '%tecnolog%' " +
                                 "OR lower(descripci_n_del_procedimiento) like '%consultor%' OR lower(descripci_n_del_procedimiento) like '%analitica%' " +
                                 "OR lower(descripci_n_del_procedimiento) like '%ingenier%')");
            }

            String whereClause = whereBase + filtroSql.toString();

            String queryParams = String.format(
                    "$limit=%d&$where=%s",
                    limite,
                    URLEncoder.encode(whereClause, StandardCharsets.UTF_8)
            );

            String fullUrl = SOCRATA_SECOP_URL + "?" + queryParams;
            log.info("Consultando API real de SECOP II: {}", fullUrl);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .header("User-Agent", "Escuela-Alertas-Bot/1.0")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Error al consultar SECOP II. Código HTTP: {}, Cuerpo: {}", response.statusCode(), response.body());
                return alertas;
            }

            JsonNode rootNode = objectMapper.readTree(response.body());
            if (rootNode.isArray()) {
                for (JsonNode item : rootNode) {
                    Alerta alerta = mapearJsonAAlerta(item);
                    if (alerta != null) {
                        alertas.add(alerta);
                    }
                }
            }

            log.info("SECOP II API retornó {} procesos relevantes procesados exitosamente.", alertas.size());

        } catch (Exception e) {
            log.error("Fallo inesperado consultando SECOP II", e);
        }

        return alertas;
    }

    private Alerta mapearJsonAAlerta(JsonNode item) {
        try {
            String entidad = item.hasNonNull("entidad") ? item.get("entidad").asText() : "Entidad Estatal";
            String referencia = item.hasNonNull("referencia_del_proceso") ? item.get("referencia_del_proceso").asText() : "Sin Ref";
            String descripcion = item.hasNonNull("descripci_n_del_procedimiento") ? item.get("descripci_n_del_procedimiento").asText() : "";
            String modalidad = item.hasNonNull("modalidad_de_contratacion") ? item.get("modalidad_de_contratacion").asText() : "Licitación / Proceso";
            double precio = item.hasNonNull("precio_base") ? item.get("precio_base").asDouble() : 0.0;

            String urlProceso = "";
            if (item.has("urlproceso")) {
                JsonNode urlNode = item.get("urlproceso");
                if (urlNode.isObject() && urlNode.has("url")) {
                    urlProceso = urlNode.get("url").asText();
                } else if (urlNode.isTextual()) {
                    urlProceso = urlNode.asText();
                }
            }

            String titulo = String.format("[%s] %s (Cuantía: $%,.0f COP)", referencia, entidad, precio);
            if (titulo.length() > 295) {
                titulo = titulo.substring(0, 292) + "...";
            }

            NivelRelevancia relevancia = (precio >= 200_000_000) ? NivelRelevancia.ALTA : NivelRelevancia.MEDIA;

            return new Alerta(
                    Herramienta.CONTRATACION_PUBLICA,
                    titulo,
                    descripcion,
                    "SECOP II (datos.gov.co)",
                    modalidad,
                    entidad,
                    urlProceso,
                    "SECOP II, contratación pública, tecnología, consultoría, ingeniería",
                    relevancia,
                    EstadoAlerta.NUEVA,
                    LocalDateTime.now()
            );
        } catch (Exception e) {
            log.warn("No se pudo mapear un registro de SECOP II", e);
            return null;
        }
    }
}
