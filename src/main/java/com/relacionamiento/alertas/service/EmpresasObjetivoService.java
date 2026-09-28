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
            "https://www.portafolio.co/rss/negocios/empresas",
            "https://www.eltiempo.com/rss/economia/empresas",
            "https://www.semana.com/rss/economia/empresas/"
    );

    // BBDD Interna simulada de Empresas Objetivo
    private static final List<String> EMPRESAS_OBJETIVO = Arrays.asList(
            "Ecopetrol", "Bancolombia", "ISA", "Grupo Aval", "Nutresa", 
            "Claro", "Avianca", "Grupo Argos", "Cementos Argos", "Enel"
    );

    // Criterios de relevancia (Semana 2)
    private static final List<String> PALABRAS_CLAVE_RELEVANTES = Arrays.asList(
            "proyecto", "inversión", "inversion", "expansión", "expansion", 
            "directivo", "reorganización", "reorganizacion", "liquidación", "liquidacion", 
            "alianza", "compra", "adquisición", "adquisicion", "crecimiento", "utilidad"
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
        
        // 100% Datos reales: Se extraen únicamente de las fuentes RSS conectadas
        // (Simulaciones históricas eliminadas por petición del usuario)

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
                    Document doc = builder.parse(new URL(feedUrl).openStream());
                    doc.getDocumentElement().normalize();
                    NodeList nList = doc.getElementsByTagName("item");

                    for (int i = 0; i < nList.getLength(); i++) {
                        Node nNode = nList.item(i);
                        if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element eElement = (Element) nNode;
                            String titulo = getTagValue("title", eElement);
                            String link = getTagValue("link", eElement);
                            String descripcion = getTagValue("description", eElement);
                            String fechaStr = getTagValue("pubDate", eElement);

                            Alerta alerta = new Alerta(
                                    Herramienta.EMPRESAS_OBJETIVO,
                                    titulo != null ? titulo : "Sin título",
                                    descripcion != null ? descripcion : "",
                                    obtenerNombreFuente(feedUrl),
                                    "Noticia de Prensa (RSS)",
                                    "Por determinar",
                                    link != null ? link : feedUrl,
                                    "noticias, rss, empresas",
                                    NivelRelevancia.MEDIA,
                                    EstadoAlerta.NUEVA,
                                    LocalDateTime.now()
                            );
                            alertas.add(alerta);
                        }
                    }
                } catch (Exception e) {
                    log.warn("No se pudo leer el RSS: " + feedUrl);
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
            // Regex para buscar palabra exacta y evitar falsos positivos de sufijos
            if (Pattern.compile("\\b" + empresa.toLowerCase() + "\\b").matcher(texto).find()) {
                return empresa;
            }
        }
        return null;
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

    private String getTagValue(String tag, Element element) {
        NodeList nodeList = element.getElementsByTagName(tag);
        if (nodeList != null && nodeList.getLength() > 0) {
            Node node = nodeList.item(0);
            if (node != null && node.getTextContent() != null) {
                // Limpiar posibles tags HTML que vienen en las descripciones de RSS
                return node.getTextContent().replaceAll("<[^>]*>", "").trim();
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
