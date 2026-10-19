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
import java.util.*;

@Service
public class SecopClient {

    private static final Logger log = LoggerFactory.getLogger(SecopClient.class);
    private static final String SOCRATA_SECOP_URL = "https://www.datos.gov.co/resource/p6dx-8zbt.json";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    // Líneas temáticas (Fuertes de la Escuela Colombiana de Ingeniería Julio Garavito)
    private static final Map<String, List<String>> FUERTES_ECI = new LinkedHashMap<>();

    static {
        FUERTES_ECI.put("Civil, Infraestructura e Interventoría", Arrays.asList(
                "interventor", "estudios y dise", "geotecnia", "estructur", "vial", "vías", "vias",
                "infraestructura", "obra civil", "obras civiles", "topograf", "paviment", "puente",
                "acueducto", "alcantarillado", "urbanismo", "construcci", "sismorresist"
        ));
        FUERTES_ECI.put("Sistemas, IA, Datos y Ciberseguridad", Arrays.asList(
                "software", "ciberseguridad", "seguridad de la informaci", "inteligencia artificial",
                "ciencia de datos", "analítica", "analitica", "big data", "nube", "cloud",
                "desarrollo tecnológico", "desarrollo tecnologico", " transformación digital",
                "transformacion digital", "sistemas de informaci", "plataforma tecnológica", "arquitectura empresarial"
        ));
        FUERTES_ECI.put("Eléctrica, Electrónica y Energía", Arrays.asList(
                "eléctric", "electric", "electrónic", "electronic", "energía", "energia", "fotovoltaic",
                "solar", "subestaci", "redes eléctricas", "automatizaci", "control industrial",
                "telecomunicacion", "biomédic", "biomedic", "alumbrado", "potencia"
        ));
        FUERTES_ECI.put("Industrial, Ambiental, Hidráulica y Gerencia", Arrays.asList(
                "consultor", "ambiental", "hidráulic", "hidraulic", "recurso hídrico", "recurso hidrico",
                "logístic", "logistic", "procesos", "calidad", "gerencia de proyectos", "planeación estratégica",
                "riesgo", "vulnerabilidad", "sostenibilidad", "economía", "financiera", "productividad"
        ));
        FUERTES_ECI.put("Educación Continua y Capacitación", Arrays.asList(
                "capacitaci", "formaci", "diplomado", "educaci", "entrenamiento", "seminario",
                "taller", "pedagógic", "pedagogic", "académic", "academic", "docencia", "curso"
        ));
    }

    // Palabras de alto valor académico/consultivo para la Escuela (elevan relevancia a ALTA)
    private static final List<String> PALABRAS_ALTO_IMPACTO_ECI = Arrays.asList(
            "consultor", "interventor", "estudios y dise", "capacitaci", "diplomado", "formaci",
            "ciberseguridad", "inteligencia artificial", "analítica", "geotecnia", "hidráulic",
            "estructural", "investigaci", "asesoría especializada", "desarrollo de software"
    );

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

            // Filtrar en tanto nombre_del_procedimiento como descripci_n_del_procedimiento según los fuertes de la Escuela
            String filtroTemas = " AND (" +
                    "upper(nombre_del_procedimiento) like '%INGENIER%' OR upper(descripci_n_del_procedimiento) like '%INGENIER%' OR " +
                    "upper(nombre_del_procedimiento) like '%CONSULTOR%' OR upper(descripci_n_del_procedimiento) like '%CONSULTOR%' OR " +
                    "upper(nombre_del_procedimiento) like '%INTERVENTOR%' OR upper(descripci_n_del_procedimiento) like '%INTERVENTOR%' OR " +
                    "upper(nombre_del_procedimiento) like '%ESTUDIOS Y DISE%' OR upper(descripci_n_del_procedimiento) like '%ESTUDIOS Y DISE%' OR " +
                    "upper(nombre_del_procedimiento) like '%SOFTWARE%' OR upper(descripci_n_del_procedimiento) like '%SOFTWARE%' OR " +
                    "upper(nombre_del_procedimiento) like '%TECNOLOG%' OR upper(nombre_del_procedimiento) like '%CIBERSEGURIDAD%' OR " +
                    "upper(nombre_del_procedimiento) like '%CAPACITACI%' OR upper(descripci_n_del_procedimiento) like '%CAPACITACI%' OR " +
                    "upper(nombre_del_procedimiento) like '%DIPLOMADO%' OR upper(nombre_del_procedimiento) like '%EDUCACI%' OR " +
                    "upper(nombre_del_procedimiento) like '%HIDRAULIC%' OR upper(nombre_del_procedimiento) like '%GEOTECNIA%' OR " +
                    "upper(nombre_del_procedimiento) like '%INFRAESTRUCTURA%' OR upper(nombre_del_procedimiento) like '%OBRA%'" +
                    ")";

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

            int fetchLimit = Math.max(limite * 2, 80);
            String queryParams = String.format(java.util.Locale.US,
                    "$limit=%d&$order=fecha_de_publicacion_del%%20DESC&$where=%s",
                    fetchLimit,
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

            // Ordenar priorizando ALTA relevancia y luego por fecha de cierre próxima / publicación reciente
            alertas.sort((a, b) -> {
                int relComp = a.getNivelRelevancia().compareTo(b.getNivelRelevancia()); // ALTA (0) antes que MEDIA (1)
                if (relComp != 0) return relComp;
                if (a.getFechaCierre() != null && b.getFechaCierre() == null) return -1;
                if (a.getFechaCierre() == null && b.getFechaCierre() != null) return 1;
                return b.getFechaPublicacion().compareTo(a.getFechaPublicacion());
            });

            log.info("SECOP II API retornó {} procesos relevantes procesados exitosamente.", alertas.size());

        } catch (Exception e) {
            log.error("Fallo inesperado consultando SECOP II", e);
        }

        return alertas;
    }

    private Alerta mapearJsonAAlerta(JsonNode item) {
        try {
            String entidad = item.hasNonNull("entidad") ? item.get("entidad").asText().trim() : "Entidad Estatal";
            String referencia = item.hasNonNull("referencia_del_proceso") ? item.get("referencia_del_proceso").asText().trim() : "Sin Ref";
            String nombreProc = item.hasNonNull("nombre_del_procedimiento") ? item.get("nombre_del_procedimiento").asText().trim() : "";
            String descProc = item.hasNonNull("descripci_n_del_procedimiento") ? item.get("descripci_n_del_procedimiento").asText().trim() : "";
            String modalidad = item.hasNonNull("modalidad_de_contratacion") ? item.get("modalidad_de_contratacion").asText().trim() : "Licitación / Proceso";
            String fase = item.hasNonNull("fase") ? item.get("fase").asText().trim() : "Abierto";
            String ciudad = item.hasNonNull("ciudad_entidad") ? item.get("ciudad_entidad").asText().trim() : "";
            String departamento = item.hasNonNull("departamento_entidad") ? item.get("departamento_entidad").asText().trim() : "";
            String duracion = item.hasNonNull("duracion") ? item.get("duracion").asText().trim() : "";
            String unidadDuracion = item.hasNonNull("unidad_de_duracion") ? item.get("unidad_de_duracion").asText().trim() : "";
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
                    fechaPublicacion = LocalDateTime.parse(item.get("fecha_de_publicacion_del").asText());
                } catch (Exception ignored) {}
            }

            LocalDateTime fechaCierre = null;
            if (item.hasNonNull("fecha_de_recepcion_de")) {
                try {
                    fechaCierre = LocalDateTime.parse(item.get("fecha_de_recepcion_de").asText());
                } catch (Exception ignored) {}
            } else if (item.hasNonNull("fecha_de_apertura_de_respuesta")) {
                try {
                    fechaCierre = LocalDateTime.parse(item.get("fecha_de_apertura_de_respuesta").asText());
                } catch (Exception ignored) {}
            }

            // Si tiene fecha de cierre registrada y ya está vencida (anterior a hoy), la descartamos
            if (fechaCierre != null && fechaCierre.toLocalDate().isBefore(LocalDateTime.now().toLocalDate())) {
                return null;
            }

            // Construir título claro con nombre del proceso y cuantía
            String baseTitulo = !nombreProc.isEmpty() ? nombreProc : descProc;
            if (baseTitulo.length() > 140) {
                baseTitulo = baseTitulo.substring(0, 137) + "...";
            }
            String titulo = String.format("[%s] %s (Cuantía: $%,.0f COP)", referencia, baseTitulo, precio);
            if (titulo.length() > 295) {
                titulo = titulo.substring(0, 292) + "...";
            }

            // Construir resumen completo y enriquecido para el desplegable de detalles
            String objetoCompleto = !descProc.isEmpty() ? descProc : nombreProc;
            StringBuilder resumenDetalle = new StringBuilder();
            resumenDetalle.append(objetoCompleto);
            resumenDetalle.append("\n\n• Modalidad: ").append(modalidad).append(" (").append(fase).append(")");
            resumenDetalle.append(" | Cuantía: $").append(String.format("%,.0f COP", precio));
            if (!duracion.isEmpty() && !duracion.equals("0")) {
                resumenDetalle.append(" | Plazo estimado: ").append(duracion).append(" ").append(unidadDuracion);
            }
            if (!ciudad.isEmpty() || !departamento.isEmpty()) {
                resumenDetalle.append(" | Ubicación: ").append(ciudad).append(departamento.isEmpty() ? "" : " (" + departamento + ")");
            }

            // Clasificar según los fuertes de la Escuela Colombiana de Ingeniería Julio Garavito
            String textoEvaluar = (nombreProc + " " + descProc + " " + modalidad).toLowerCase();
            List<String> areasDetectadas = new ArrayList<>();
            List<String> keywordsEncontradas = new ArrayList<>();

            for (Map.Entry<String, List<String>> entry : FUERTES_ECI.entrySet()) {
                for (String kw : entry.getValue()) {
                    if (textoEvaluar.contains(kw.toLowerCase())) {
                        if (!areasDetectadas.contains(entry.getKey())) {
                            areasDetectadas.add(entry.getKey());
                        }
                        String kwLimpia = kw.trim();
                        if (!keywordsEncontradas.contains(kwLimpia) && keywordsEncontradas.size() < 5) {
                            keywordsEncontradas.add(kwLimpia);
                        }
                    }
                }
            }

            if (areasDetectadas.isEmpty()) {
                areasDetectadas.add("Ingeniería y Consultoría General");
            }

            boolean esAltoImpactoAcademico = PALABRAS_ALTO_IMPACTO_ECI.stream()
                    .anyMatch(kw -> textoEvaluar.contains(kw.toLowerCase()))
                    || modalidad.toLowerCase().contains("méritos")
                    || modalidad.toLowerCase().contains("meritos");

            NivelRelevancia relevancia;
            if (esAltoImpactoAcademico || areasDetectadas.size() >= 2) {
                relevancia = NivelRelevancia.ALTA;
            } else {
                relevancia = NivelRelevancia.MEDIA;
            }

            String areaPrincipal = String.join(" | ", areasDetectadas);
            String etiquetasPalabras = areaPrincipal + (keywordsEncontradas.isEmpty() ? "" : " • Claves: " + String.join(", ", keywordsEncontradas));
            if (etiquetasPalabras.length() > 490) {
                etiquetasPalabras = etiquetasPalabras.substring(0, 487) + "...";
            }

            Alerta alerta = new Alerta(
                    Herramienta.CONTRATACION_PUBLICA,
                    titulo,
                    resumenDetalle.toString(),
                    "SECOP II (datos.gov.co)",
                    modalidad,
                    entidad,
                    urlProceso,
                    etiquetasPalabras,
                    relevancia,
                    EstadoAlerta.NUEVA,
                    fechaPublicacion
            );
            alerta.setFechaCierre(fechaCierre);
            return alerta;
        } catch (Exception e) {
            log.warn("No se pudo mapear un registro de SECOP II", e);
            return null;
        }
    }
}
