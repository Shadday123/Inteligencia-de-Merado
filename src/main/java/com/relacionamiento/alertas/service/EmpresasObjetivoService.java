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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class EmpresasObjetivoService {

    private static final Logger log = LoggerFactory.getLogger(EmpresasObjetivoService.class);

    private final AlertaRepository alertaRepository;

    // Fuentes definidas en el documento de Arquitectura Semana 2
    private static final List<String> FUENTES_RSS = Arrays.asList(
            "https://news.google.com/rss/search?q=Ecopetrol+OR+Bancolombia+OR+ISA+OR+Exito+OR+Sura+OR+EPM+when:7d&hl=es-419&gl=CO&ceid=CO:es-419",
            "https://www.portafolio.co/rss/negocios/empresas",
            "https://www.eltiempo.com/rss/economia/empresas",
            "https://www.semana.com/rss/economia/empresas/"
    );

    private static final List<String> EMPRESAS_OBJETIVO = Arrays.asList(
            "Ecopetrol", "Bancolombia", "ISA", "Grupo Aval", "Nutresa", 
            "Argos", "EPM", "Sura", "Éxito", "Avianca", "Enel", "Claro", "Celsia", "Terpel", "Postobón"
    );

    // Criterios de relevancia (Semana 2) + Ampliados para prototipo
    private static final List<String> PALABRAS_CLAVE_RELEVANTES = Arrays.asList(
            "proyecto", "inversión", "inversion", "expansión", "expansion", 
            "directivo", "reorganización", "reorganizacion", "liquidación", "liquidacion", 
            "alianza", "compra", "adquisición", "adquisicion", "crecimiento", "utilidad",
            "millones", "dólar", "dolar", "tasa", "mercado", "precio", "nuevo", "acciones"
    );

    public EmpresasObjetivoService(AlertaRepository alertaRepository) {
        this.alertaRepository = alertaRepository;
    }

    /**
     * Extrae noticias de los RSS, aplica filtros de relevancia, clasifica falsos positivos
     * y genera el reporte de prueba exigido para la Semana 4.
     */
    public Map<String, Object> generarReportePrueba() {
        List<Alerta> noticiasBrutas = extraerNoticiasRSS();
        
        List<Alerta> relevantes = new ArrayList<>();
        List<Alerta> nuevasParaGuardar = new ArrayList<>();
        List<Map<String, String>> falsosPositivos = new ArrayList<>();

        // Motor de Filtrado y Criterios de Relevancia
        for (Alerta noticia : noticiasBrutas) {
            String empresaDetectada = contieneEmpresaObjetivo(noticia.getTitulo(), noticia.getDescripcion());
            
            if (empresaDetectada != null) {
                if (esRelevante(noticia.getTitulo(), noticia.getDescripcion())) {
                    noticia.setEmpresaEntidadRelacionada(empresaDetectada);
                    
                    // Siempre mostrar en el reporte
                    relevantes.add(noticia);
                    
                    // Solo guardar si no está duplicada en BD
                    if (!alertaRepository.existsByUrlOrigen(noticia.getUrlOrigen())) {
                        nuevasParaGuardar.add(noticia);
                    }
                } else {
                    // Es un falso positivo: Menciona a la empresa, pero no es de impacto económico/negocios
                    Map<String, String> fp = new HashMap<>();
                    fp.put("titulo", noticia.getTitulo());
                    fp.put("razonDescarte", "Menciona a " + empresaDetectada + " pero no contiene palabras clave de impacto (inversión, proyectos, etc.)");
                    falsosPositivos.add(fp);
                }
            }
        }

        // Guardar las nuevas en la base de datos central
        alertaRepository.saveAll(nuevasParaGuardar);

        // Armar el reporte de la Semana 4
        Map<String, Object> reporte = new LinkedHashMap<>();
        reporte.put("titulo", "Reporte de Prueba: Monitoreo de Empresas Objetivo v1");
        reporte.put("fuentesAnalizadas", "Portafolio (RSS), El Tiempo (RSS), Semana (RSS)");
        reporte.put("empresasMonitoreadas", EMPRESAS_OBJETIVO);
        reporte.put("totalNoticiasAnalizadas", noticiasBrutas.size());
        reporte.put("mencionesRelevantesDetectadas", relevantes.size());
        reporte.put("falsosPositivosDescartados", falsosPositivos.size());
        reporte.put("ejemplosFalsosPositivos", falsosPositivos.stream().limit(5).collect(Collectors.toList()));
        reporte.put("alertasRelevantes", relevantes);

        return reporte;
    }

    private List<Alerta> extraerNoticiasRSS() {
        List<Alerta> alertas = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            for (String feedUrl : FUENTES_RSS) {
                try {
                    java.net.URLConnection connection = new URL(feedUrl).openConnection();
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
                            String fechaStr = getTagValue("pubDate", eElement);

                            String imageUrl = extraerImagen(eElement, rawDescripcion);

                            Alerta alerta = new Alerta(
                                    Herramienta.EMPRESAS_OBJETIVO,
                                    titulo != null ? titulo : "Sin título",
                                    descripcion,
                                    obtenerNombreFuente(feedUrl),
                                    "Noticia de Prensa (RSS)",
                                    "Por determinar",
                                    link != null ? link : feedUrl,
                                    "noticias, rss, empresas",
                                    NivelRelevancia.MEDIA,
                                    EstadoAlerta.NUEVA,
                                    LocalDateTime.now()
                            );
                            alerta.setImagenUrl(imageUrl);
                            alertas.add(alerta);
                        }
                    }
                } catch (Exception e) {
                    log.warn("No se pudo leer el RSS: " + feedUrl + " - Error: " + e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error inicializando lector RSS", e);
        }
        return alertas;
    }

    private String contieneEmpresaObjetivo(String titulo, String descripcion) {
        String texto = (titulo + " " + descripcion).toLowerCase();
        for (String empresa : EMPRESAS_OBJETIVO) {
            if (texto.contains(empresa.toLowerCase())) {
                return empresa;
            }
        }
        return null; // Si no hay empresa, devolvemos nulo (filtro real)
    }

    private boolean esRelevante(String titulo, String descripcion) {
        String texto = (titulo + " " + descripcion).toLowerCase();
        for (String palabra : PALABRAS_CLAVE_RELEVANTES) {
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
        // Opción 1: <media:content url="...">
        NodeList mediaList = eElement.getElementsByTagName("media:content");
        if (mediaList != null && mediaList.getLength() > 0) {
            Element mediaElement = (Element) mediaList.item(0);
            return mediaElement.getAttribute("url");
        }
        
        // Opción 2: <enclosure url="..." type="image/...">
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
        
        // Opción 3: <img> tag en la description
        if (rawDescripcion != null) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>").matcher(rawDescripcion);
            if (m.find()) {
                return m.group(1);
            }
        }
        return null;
    }

    private String obtenerNombreFuente(String url) {
        if (url.contains("portafolio")) return "Portafolio Economía";
        if (url.contains("eltiempo")) return "El Tiempo Economía";
        if (url.contains("semana")) return "Revista Semana";
        return "Fuente Externa RSS";
    }

}
