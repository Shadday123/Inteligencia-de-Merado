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
            // Filtrar por estado, fecha no nula y aplicar cuantía
            String whereBase = String.format(java.util.Locale.US,
                    "estado_del_procedimiento in ('Publicado', 'Presentación de ofertas', 'Presentación de oferta', 'Abierto') AND precio_base >= %f AND fecha_de_publicacion_del IS NOT NULL",
                    cuantiaMinima
            );

            // Filtrar por palabras clave de interés para la Escuela
            String filtroTemas = " AND (upper(nombre_del_procedimiento) like '%INGENIER%' OR upper(nombre_del_procedimiento) like '%CONSULTOR%' OR upper(nombre_del_procedimiento) like '%INTERVENTOR%' OR upper(nombre_del_procedimiento) like '%ESTUDIOS Y DISE%' OR upper(nombre_del_procedimiento) like '%OBRA%' OR upper(nombre_del_procedimiento) like '%SOFTWARE%' OR upper(nombre_del_procedimiento) like '%TECNOLOG%' OR upper(nombre_del_procedimiento) like '%CAPACITACI%' OR upper(nombre_del_procedimiento) like '%FORMACI%' OR upper(nombre_del_procedimiento) like '%EDUCACI%')";

            StringBuilder filtroSql = new StringBuilder();
            if (entidad != null) {
                filtroSql.append(" AND lower(entidad) like '%")
                         .append(entidad.getNombreReal().toLowerCase().replace("ministerio de ", "").replace(" nacional", ""))
                         .append("%'");
            } else if (sector != null) {
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

            String whereClause = whereBase + filtroTemas + filtroSql.toString();

            String queryParams = String.format(java.util.Locale.US,
                    "$limit=%d&$order=fecha_de_publicacion_del%%20DESC&$where=%s",
                    limite,
                    URLEncoder.encode(whereClause, StandardCharsets.UTF_8).replace("+", "%20")
            );

            String fullUrl = SOCRATA_SECOP_URL + "?" + queryParams;
            log.info("Consultando API real de SECOP II: {}", fullUrl);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .header("User-Agent", "Escuela-Alertas-Bot/1.0")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(20))
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

            LocalDateTime fechaPublicacion = LocalDateTime.now();
            if (item.hasNonNull("fecha_de_publicacion_del")) {
                try {
                    String f = item.get("fecha_de_publicacion_del").asText();
                    fechaPublicacion = LocalDateTime.parse(f);
                } catch (Exception e) {
                    // fallback to now
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
                    fechaPublicacion
            );
        } catch (Exception e) {
            log.warn("No se pudo mapear un registro de SECOP II", e);
            return null;
        }
    }
}
