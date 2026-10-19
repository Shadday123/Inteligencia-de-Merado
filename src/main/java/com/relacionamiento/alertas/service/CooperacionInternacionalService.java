package com.relacionamiento.alertas.service;

import com.relacionamiento.alertas.domain.EstadoAlerta;
import com.relacionamiento.alertas.domain.Herramienta;
import com.relacionamiento.alertas.domain.NivelRelevancia;
import com.relacionamiento.alertas.entity.Alerta;
import com.relacionamiento.alertas.repository.AlertaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CooperacionInternacionalService {

    private static final Logger log = LoggerFactory.getLogger(CooperacionInternacionalService.class);

    private final AlertaRepository alertaRepository;

    // Fuentes RSS reales y activas (OCHA ReliefWeb + Google News segmentado por Agencias, Cooperación y Gestión del Riesgo/Reconstrucción)
    private static final List<String> FUENTES_RSS_REALES = Arrays.asList(
            // 1. OCHA / ReliefWeb Colombia (Reportes humanitarios y desastres en inglés y español)
            "https://reliefweb.int/updates/rss.xml?country=68",
            // 2. Agencias Multilaterales y de Cooperación en Colombia (APC Colombia, BID, CAF, Banco Mundial, Unión Europea, USAID, PNUD)
            "https://news.google.com/rss/search?q=%22APC+Colombia%22+OR+%22Banco+Interamericano+de+Desarrollo%22+OR+%22CAF%22+OR+%22Banco+Mundial%22+OR+%22Cooperaci%C3%B3n+Internacional%22+OR+%22Uni%C3%B3n+Europea%22+Colombia&hl=es-419&gl=CO&ceid=CO:es-419",
            // 3. Convocatorias de becas, financiación educativa, investigación, ciencia e innovación en Colombia
            "https://news.google.com/rss/search?q=(convocatoria+OR+subvenci%C3%B3n+OR+financiaci%C3%B3n+OR+becas+OR+fondos)+(universidades+OR+investigaci%C3%B3n+OR+ingenier%C3%ADa+OR+innovaci%C3%B3n+OR+educaci%C3%B3n)+Colombia&hl=es-419&gl=CO&ceid=CO:es-419",
            // 4. UNGRD, DNP, Reconstrucción, Sismos, Emergencias e Infraestructura Resiliente en Colombia
            "https://news.google.com/rss/search?q=(%22UNGRD%22+OR+%22DNP%22+OR+reconstrucci%C3%B3n+OR+sismo+OR+terremoto+OR+%22gesti%C3%B3n+del+riesgo%22+OR+damnificados+OR+emergencia)+infraestructura+Colombia&hl=es-419&gl=CO&ceid=CO:es-419"
    );

    private static final Map<String, List<String>> AGENCIAS_MONITOREADAS = new LinkedHashMap<>();

    static {
        AGENCIAS_MONITOREADAS.put("APC Colombia", Arrays.asList("apc colombia", "agencia presidencial de cooperación"));
        AGENCIAS_MONITOREADAS.put("BID (Banco Interamericano)", Arrays.asList("banco interamericano", " bid "));
        AGENCIAS_MONITOREADAS.put("CAF (Banco de Desarrollo)", Arrays.asList("banco de desarrollo de américa latina", " caf "));
        AGENCIAS_MONITOREADAS.put("Banco Mundial", Arrays.asList("banco mundial", "world bank"));
        AGENCIAS_MONITOREADAS.put("OCHA / Naciones Unidas", Arrays.asList("ocha", "naciones unidas", "onu", "pnud", "unicef", "acnur", "reliefweb"));
        AGENCIAS_MONITOREADAS.put("UNGRD (Gestión del Riesgo)", Arrays.asList("ungrd", "gestión del riesgo", "gestion del riesgo"));
        AGENCIAS_MONITOREADAS.put("DNP / Gobierno Nacional", Arrays.asList("dnp", "departamento nacional de planeación", "ministerio", "presidencia"));
        AGENCIAS_MONITOREADAS.put("Unión Europea", Arrays.asList("unión europea", "union europea", "embajada"));
        AGENCIAS_MONITOREADAS.put("USAID / Cooperación Bilateral", Arrays.asList("usaid", "cooperación internacional", "cooperacion internacional", "icetex", "minciencias"));
    }

    // Criterios de relevancia (Español + Inglés porque ReliefWeb publica en ambos idiomas)
    private static final List<String> PALABRAS_CLAVE_COOPERACION = Arrays.asList(
            "convocatoria", "fondo", "financiamiento", "financiación", "financiacion", "cooperación", "cooperacion",
            "donación", "donacion", "beca", "desarrollo", "subvención", "subvencion", "alianza", "apoyo",
            "inversión", "inversion", "proyecto", "programa", "educación", "educacion", "investigación",
            "innovación", "sostenible", "climático", "humanitarian", "funding", "response", "support", "colombia"
    );

    private static final List<String> PALABRAS_CLAVE_FONDO_MILAGRO = Arrays.asList(
            "fondo milagro", "reconstrucción", "reconstruccion", "terremoto", "sismo", "desastre", "emergencia",
            "infraestructura", "vivienda", "damnificados", "ungrd", "gestión del riesgo", "riesgo", "inundación",
            "deslizamiento", "humanitaria", "earthquake", "disaster", "flood", "emergency"
    );

    public CooperacionInternacionalService(AlertaRepository alertaRepository) {
        this.alertaRepository = alertaRepository;
    }

    public Map<String, Object> generarReportePrueba() {
        List<Alerta> oportunidadesBrutas = extraerOportunidadesRealesRSS();

        List<Alerta> relevantes = new ArrayList<>();
        List<Alerta> nuevasParaGuardar = new ArrayList<>();
        List<Map<String, String>> falsosPositivos = new ArrayList<>();
        Set<String> titulosVistos = new HashSet<>();

        for (Alerta oportunidad : oportunidadesBrutas) {
            String tituloNorm = oportunidad.getTitulo() != null ? oportunidad.getTitulo().trim().toLowerCase() : "";
            if (tituloNorm.isEmpty() || titulosVistos.contains(tituloNorm)) {
                continue;
            }

            String agencia = detectarAgencia(oportunidad.getTitulo(), oportunidad.getDescripcion(), oportunidad.getFuente());

            boolean esCooperacion = evaluarRelevancia(oportunidad.getTitulo(), oportunidad.getDescripcion(), PALABRAS_CLAVE_COOPERACION);
            boolean esFondoMilagro = evaluarRelevancia(oportunidad.getTitulo(), oportunidad.getDescripcion(), PALABRAS_CLAVE_FONDO_MILAGRO);

            if (esCooperacion || esFondoMilagro) {
                titulosVistos.add(tituloNorm);
                oportunidad.setEmpresaEntidadRelacionada(agencia);

                if (esFondoMilagro) {
                    oportunidad.setNivelRelevancia(NivelRelevancia.ALTA);
                    oportunidad.setPalabrasClaveDetectadas("Gestión de Desastres / Reconstrucción / Urgencia");
                } else {
                    oportunidad.setNivelRelevancia(NivelRelevancia.MEDIA);
                    oportunidad.setPalabrasClaveDetectadas("Cooperación Internacional / Convocatoria / Fondos");
                }

                relevantes.add(oportunidad);

                if (!alertaRepository.existsByUrlOrigen(oportunidad.getUrlOrigen())) {
                    nuevasParaGuardar.add(oportunidad);
                }
            } else {
                Map<String, String> fp = new HashMap<>();
                fp.put("titulo", oportunidad.getTitulo());
                fp.put("razonDescarte", "Publicación de " + agencia + " sin relación directa con convocatorias, fondos o atención de emergencias.");
                falsosPositivos.add(fp);
            }
        }

        alertaRepository.saveAll(nuevasParaGuardar);

        Map<String, Object> reporte = new LinkedHashMap<>();
        reporte.put("titulo", "Reporte de Cooperación Internacional y Reconstrucción / Emergencias");
        reporte.put("fuentesAnalizadas", "OCHA ReliefWeb (RSS), Google News Cooperación Multilateral (APC, BID, CAF, BM, UE, UNGRD)");
        reporte.put("totalPublicacionesAnalizadas", oportunidadesBrutas.size());
        reporte.put("oportunidadesRelevantesDetectadas", relevantes.size());
        reporte.put("falsosPositivosDescartados", falsosPositivos.size());
        reporte.put("ejemplosFalsosPositivos", falsosPositivos.stream().limit(5).collect(Collectors.toList()));
        reporte.put("alertasRelevantes", relevantes);

        return reporte;
    }

    private List<Alerta> extraerOportunidadesRealesRSS() {
        List<Alerta> alertas = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            for (String feedUrl : FUENTES_RSS_REALES) {
                try {
                    java.net.URLConnection connection = new URL(feedUrl).openConnection();
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(10000);
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                    Document doc = builder.parse(connection.getInputStream());
                    doc.getDocumentElement().normalize();
                    NodeList nList = doc.getElementsByTagName("item");

                    for (int i = 0; i < nList.getLength(); i++) {
                        Node nNode = nList.item(i);
                        if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element eElement = (Element) nNode;
                            String titulo = getTagValue("title", eElement);
                            String link = getTagValue("link", eElement);
                            String rawDescripcion = getRawTagValue("description", eElement);
                            String descripcion = rawDescripcion != null ? rawDescripcion.replaceAll("<[^>]*>", "").trim() : "";
                            String sourceTag = getTagValue("source", eElement);
                            String imageUrl = extraerImagen(eElement, rawDescripcion);

                            Alerta alerta = new Alerta(
                                    Herramienta.COOPERACION_INTERNACIONAL,
                                    titulo != null ? titulo : "Sin título",
                                    descripcion,
                                    sourceTag != null && !sourceTag.isEmpty() ? sourceTag : obtenerNombreFuente(feedUrl),
                                    "Cooperación / Fondos (RSS)",
                                    "Por determinar",
                                    link != null ? link : feedUrl,
                                    "cooperacion, rss",
                                    NivelRelevancia.MEDIA,
                                    EstadoAlerta.NUEVA,
                                    LocalDateTime.now()
                            );
                            alerta.setImagenUrl(imageUrl);
                            alertas.add(alerta);
                        }
                    }
                } catch (Exception e) {
                    log.warn("No se pudo leer el RSS de Cooperación: " + feedUrl);
                }
            }
        } catch (Exception e) {
            log.error("Error inicializando lector RSS Cooperacion", e);
        }
        return alertas;
    }

    private String detectarAgencia(String titulo, String descripcion, String fuente) {
        String texto = (" " + titulo + " " + descripcion + " " + fuente + " ").toLowerCase();
        for (Map.Entry<String, List<String>> entry : AGENCIAS_MONITOREADAS.entrySet()) {
            for (String kw : entry.getValue()) {
                if (texto.contains(kw.toLowerCase())) {
                    return entry.getKey();
                }
            }
        }
        return "Cooperación Multilateral / Nacional";
    }

    private boolean evaluarRelevancia(String titulo, String descripcion, List<String> palabrasClave) {
        String texto = (titulo + " " + descripcion).toLowerCase();
        for (String palabra : palabrasClave) {
            if (texto.contains(palabra.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private String getRawTagValue(String tag, Element element) {
        NodeList nodeList = element.getElementsByTagName(tag);
        if (nodeList != null && nodeList.getLength() > 0) {
            Node node = nodeList.item(0);
            if (node != null && node.getTextContent() != null) {
                return node.getTextContent();
            }
        }
        return null;
    }

    private String getTagValue(String tag, Element element) {
        String raw = getRawTagValue(tag, element);
        return raw != null ? raw.replaceAll("<[^>]*>", "").trim() : null;
    }

    private String extraerImagen(Element eElement, String rawDescripcion) {
        NodeList mediaList = eElement.getElementsByTagName("media:content");
        if (mediaList != null && mediaList.getLength() > 0) {
            Element mediaElement = (Element) mediaList.item(0);
            return mediaElement.getAttribute("url");
        }
        
        NodeList enclosureList = eElement.getElementsByTagName("enclosure");
        if (enclosureList != null && enclosureList.getLength() > 0) {
            for (int i = 0; i < enclosureList.getLength(); i++) {
                Element enclosure = (Element) enclosureList.item(i);
                String type = enclosure.getAttribute("type");
                if (type != null && type.startsWith("image")) {
                    return enclosure.getAttribute("url");
                }
            }
        }
        
        if (rawDescripcion != null) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>").matcher(rawDescripcion);
            if (m.find()) {
                return m.group(1);
            }
        }
        return null;
    }

    private String obtenerNombreFuente(String url) {
        if (url.contains("reliefweb")) return "OCHA / ReliefWeb";
        if (url.contains("portafolio")) return "Portafolio";
        if (url.contains("eltiempo")) return "El Tiempo";
        return "Fuente Externa RSS";
    }

}
