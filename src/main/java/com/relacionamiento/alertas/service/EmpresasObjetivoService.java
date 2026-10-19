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

    // Configuración enriquecida por empresa: nombre, categoría, ubicación oficial, alias de la empresa,
    // palabras de contexto sectorial (para confirmar que es esa empresa específica) y términos geográficos/planta
    public record EmpresaConfig(
            String nombreMostrar,
            String categoria,
            String ubicacion,
            List<String> aliasBusqueda,
            List<String> contextoIdentidad,
            List<String> terminosUbicacion
    ) {}

    private static final List<String> PALABRAS_EXCLUSION_HOMONIMOS = Arrays.asList(
            "miss universe", "miss universo", "reinado", "reina de belleza", "entregar su corona", "triple corona",
            "corona campeón", "coronó campeón", "se corona", "abierto de golf", "fútbol", "futbol", "liga betplay",
            "abelardo de la espriella", "elecciones", "candidato presidencial", "barrio bavaria", "acueducto de bogotá"
    );

    private static final List<String> PALABRAS_EXCLUSION_EXTRANJERO = Arrays.asList(
            "méxico", "mexico", "acapulco", "monterrey", "bolsa de méxico", "bmv", "costa rica",
            "aje perú", "en perú", "en bolivia", "visita express de kast", "en chile", "en argentina",
            "en españa", "great american beer"
    );

    private static final List<String> UBICACIONES_COLOMBIA_NACIONAL = Arrays.asList(
            "Colombia", "Bogotá"
    );

    private static final List<EmpresaConfig> CONFIG_EMPRESAS = Arrays.asList(
            // --- PROGRAMA: LA ESCUELA TE VISITA (1 nombre principal + 1 sinónimo de empresa, y 1 ubicación principal + 1 sinónimo de ubicación) ---
            new EmpresaConfig("Coca-Cola FEMSA", "LA_ESCUELA_TE_VISITA", "Tocancipá – Vda. Canavita",
                    Arrays.asList("Coca-Cola FEMSA", "Coca-Cola"),
                    Collections.emptyList(),
                    Arrays.asList("Tocancipá", "Canavita")),
            new EmpresaConfig("Bavaria – AB InBev", "LA_ESCUELA_TE_VISITA", "Tocancipá – Vda. Canavita",
                    Arrays.asList("Bavaria", "AB InBev"),
                    Collections.emptyList(),
                    Arrays.asList("Tocancipá", "Canavita")),
            new EmpresaConfig("Belcorp (Ésika, L'BEL, Cyzone)", "LA_ESCUELA_TE_VISITA", "Tocancipá – Vda. Canavita",
                    Arrays.asList("Belcorp", "Ésika"),
                    Collections.emptyList(),
                    Arrays.asList("Tocancipá", "Canavita")),
            new EmpresaConfig("Quala", "LA_ESCUELA_TE_VISITA", "Tocancipá – Vda. Tibitó",
                    Arrays.asList("Quala", "Quala Colombia"),
                    Collections.emptyList(),
                    Arrays.asList("Tocancipá", "Tibitó")),
            new EmpresaConfig("Aceros Diaco – Gerdau", "LA_ESCUELA_TE_VISITA", "Tocancipá – Vda. Tibitó",
                    Arrays.asList("Aceros Diaco", "Gerdau Diaco"),
                    Collections.emptyList(),
                    Arrays.asList("Tocancipá", "Tibitó")),
            new EmpresaConfig("Alpina Productos Alimenticios", "LA_ESCUELA_TE_VISITA", "Sopó, Cundinamarca",
                    Arrays.asList("Alpina", "Alpina Productos Alimenticios"),
                    Collections.emptyList(),
                    Arrays.asList("Sopó", "Cundinamarca")),
            new EmpresaConfig("Corona Industrial – Prestigio II", "LA_ESCUELA_TE_VISITA", "Sopó, Cundinamarca",
                    Arrays.asList("Corona Industrial", "Organización Corona"),
                    Collections.emptyList(),
                    Arrays.asList("Sopó", "Cundinamarca")),
            new EmpresaConfig("Sika Colombia", "LA_ESCUELA_TE_VISITA", "Tocancipá – Vda. Canavita",
                    Arrays.asList("Sika Colombia", "Sika"),
                    Collections.emptyList(),
                    Arrays.asList("Tocancipá", "Canavita")),
            new EmpresaConfig("Linde Colombia", "LA_ESCUELA_TE_VISITA", "Tocancipá – Vda. Canavita",
                    Arrays.asList("Linde Colombia", "Linde"),
                    Collections.emptyList(),
                    Arrays.asList("Tocancipá", "Canavita")),
            new EmpresaConfig("Alquería", "LA_ESCUELA_TE_VISITA", "Cajicá, Cundinamarca",
                    Arrays.asList("Alquería", "Lácteos Alquería"),
                    Collections.emptyList(),
                    Arrays.asList("Cajicá", "Cundinamarca")),

            // --- GRANDES EMPRESAS NACIONALES (1 nombre principal + 1 sinónimo de empresa, y Colombia + Bogotá como ubicación) ---
            new EmpresaConfig("Ecopetrol", "GRANDES_NACIONALES", "Colombia", Arrays.asList("Ecopetrol", "Grupo Ecopetrol"), Collections.emptyList(), UBICACIONES_COLOMBIA_NACIONAL),
            new EmpresaConfig("Bancolombia", "GRANDES_NACIONALES", "Colombia", Arrays.asList("Bancolombia", "Grupo Bancolombia"), Collections.emptyList(), UBICACIONES_COLOMBIA_NACIONAL),
            new EmpresaConfig("ISA", "GRANDES_NACIONALES", "Colombia", Arrays.asList("ISA", "Interconexión Eléctrica"), Collections.emptyList(), UBICACIONES_COLOMBIA_NACIONAL),
            new EmpresaConfig("Grupo Aval", "GRANDES_NACIONALES", "Colombia", Arrays.asList("Grupo Aval", "Aval"), Collections.emptyList(), UBICACIONES_COLOMBIA_NACIONAL),
            new EmpresaConfig("Nutresa", "GRANDES_NACIONALES", "Colombia", Arrays.asList("Nutresa", "Grupo Nutresa"), Collections.emptyList(), UBICACIONES_COLOMBIA_NACIONAL),
            new EmpresaConfig("Argos", "GRANDES_NACIONALES", "Colombia", Arrays.asList("Argos", "Cementos Argos"), Collections.emptyList(), UBICACIONES_COLOMBIA_NACIONAL),
            new EmpresaConfig("EPM", "GRANDES_NACIONALES", "Colombia", Arrays.asList("EPM", "Empresas Públicas de Medellín"), Collections.emptyList(), UBICACIONES_COLOMBIA_NACIONAL),
            new EmpresaConfig("Sura", "GRANDES_NACIONALES", "Colombia", Arrays.asList("Sura", "Grupo Sura"), Collections.emptyList(), UBICACIONES_COLOMBIA_NACIONAL),
            new EmpresaConfig("Grupo Éxito", "GRANDES_NACIONALES", "Colombia", Arrays.asList("Grupo Éxito", "Almacenes Éxito"), Collections.emptyList(), UBICACIONES_COLOMBIA_NACIONAL),
            new EmpresaConfig("Avianca", "GRANDES_NACIONALES", "Colombia", Arrays.asList("Avianca", "Grupo Avianca"), Collections.emptyList(), UBICACIONES_COLOMBIA_NACIONAL),
            new EmpresaConfig("Enel Colombia", "GRANDES_NACIONALES", "Colombia", Arrays.asList("Enel Colombia", "Enel Codensa"), Collections.emptyList(), UBICACIONES_COLOMBIA_NACIONAL),
            new EmpresaConfig("Claro Colombia", "GRANDES_NACIONALES", "Colombia", Arrays.asList("Claro Colombia", "Claro"), Collections.emptyList(), UBICACIONES_COLOMBIA_NACIONAL),
            new EmpresaConfig("Celsia", "GRANDES_NACIONALES", "Colombia", Arrays.asList("Celsia", "Celsia Energía"), Collections.emptyList(), UBICACIONES_COLOMBIA_NACIONAL),
            new EmpresaConfig("Terpel", "GRANDES_NACIONALES", "Colombia", Arrays.asList("Terpel", "Organización Terpel"), Collections.emptyList(), UBICACIONES_COLOMBIA_NACIONAL),
            new EmpresaConfig("Postobón", "GRANDES_NACIONALES", "Colombia", Arrays.asList("Postobón", "Organización Ardila Lülle"), Collections.emptyList(), UBICACIONES_COLOMBIA_NACIONAL)
    );

    public EmpresasObjetivoService(AlertaRepository alertaRepository) {
        this.alertaRepository = alertaRepository;
    }

    public Map<String, Object> generarReportePrueba() {
        return generarReportePrueba(null, null);
    }

    public Map<String, Object> generarReportePrueba(String empresaFiltro, String categoriaFiltro) {
        List<Alerta> noticiasBrutas = new ArrayList<>();

        EmpresaConfig empresaPrioritaria = (empresaFiltro != null && !empresaFiltro.isBlank())
                ? buscarEmpresaConfigPorNombre(empresaFiltro)
                : null;

        if (empresaPrioritaria != null) {
            noticiasBrutas.addAll(extraerNoticiasRssPorEmpresa(empresaPrioritaria));
        } else {
            List<EmpresaConfig> empresasAConsultar = CONFIG_EMPRESAS.stream()
                    .filter(e -> categoriaFiltro == null || categoriaFiltro.isBlank() || e.categoria().equalsIgnoreCase(categoriaFiltro))
                    .collect(Collectors.toList());

            List<java.util.concurrent.CompletableFuture<List<Alerta>>> futuros = empresasAConsultar.stream()
                    .map(emp -> java.util.concurrent.CompletableFuture.supplyAsync(() -> extraerNoticiasRssResumenPorEmpresa(emp)))
                    .collect(Collectors.toList());

            for (java.util.concurrent.CompletableFuture<List<Alerta>> f : futuros) {
                try {
                    noticiasBrutas.addAll(f.join());
                } catch (Exception ignored) {}
            }
        }

        List<Alerta> relevantes = new ArrayList<>();
        List<Alerta> nuevasParaGuardar = new ArrayList<>();
        List<Map<String, String>> falsosPositivos = new ArrayList<>();
        Set<String> titulosVistos = new HashSet<>();

        for (Alerta noticia : noticiasBrutas) {
            String tituloNorm = noticia.getTitulo() != null ? noticia.getTitulo().trim().toLowerCase() : "";
            if (tituloNorm.isEmpty() || titulosVistos.contains(tituloNorm)) {
                continue;
            }

            String texto = (" " + noticia.getTitulo() + " " + noticia.getDescripcion() + " ").toLowerCase();

            EmpresaConfig candidata = null;
            if (empresaPrioritaria != null) {
                candidata = empresaPrioritaria;
            } else if (noticia.getEmpresaEntidadRelacionada() != null && !noticia.getEmpresaEntidadRelacionada().equals("Por determinar")) {
                candidata = buscarEmpresaConfigPorNombre(noticia.getEmpresaEntidadRelacionada());
            } else {
                candidata = detectarEmpresaConIdentidad(noticia.getTitulo(), noticia.getDescripcion(), null);
            }

            EmpresaConfig empresaDetectada = null;
            if (candidata != null
                    && !esNoticiaExcluidaOExtranjera(texto, candidata)
                    && cumpleIdentidadEmpresa(texto, candidata)) {
                empresaDetectada = candidata;
            }

            if (empresaDetectada != null) {
                titulosVistos.add(tituloNorm);

                String etiquetaEmpresa = "LA_ESCUELA_TE_VISITA".equals(empresaDetectada.categoria())
                        ? empresaDetectada.nombreMostrar() + " (" + empresaDetectada.ubicacion() + ")"
                        : empresaDetectada.nombreMostrar();

                noticia.setEmpresaEntidadRelacionada(etiquetaEmpresa);

                boolean mencionaUbicacion = verificaMencionUbicacion(noticia.getTitulo(), noticia.getDescripcion(), empresaDetectada);
                String sinonimoEmpresaUsado = obtenerSinonimoMarcaDetectado(noticia.getTitulo(), noticia.getDescripcion(), empresaDetectada);
                String sinonimoLugarUsado = obtenerSinonimoUbicacionDetectado(noticia.getTitulo(), noticia.getDescripcion(), empresaDetectada);
                String parPredefinido = noticia.getPalabrasClaveDetectadas();
                boolean tieneParBusqueda = (parPredefinido != null && parPredefinido.startsWith("Palabras clave: "));

                if ("LA_ESCUELA_TE_VISITA".equals(empresaDetectada.categoria())) {
                    noticia.setTipoInformacion(mencionaUbicacion ? "LA_ESCUELA_TE_VISITA_UBICACION_CONFIRMADA" : "LA_ESCUELA_TE_VISITA");
                    noticia.setNivelRelevancia(NivelRelevancia.ALTA);
                    if (!tieneParBusqueda || mencionaUbicacion) {
                        noticia.setPalabrasClaveDetectadas("Palabras clave: " + sinonimoEmpresaUsado + " + " + sinonimoLugarUsado);
                    }
                } else {
                    noticia.setTipoInformacion("GRANDES_NACIONALES");
                    noticia.setNivelRelevancia(NivelRelevancia.MEDIA);
                    if (!tieneParBusqueda) {
                        noticia.setPalabrasClaveDetectadas("Palabras clave: " + sinonimoEmpresaUsado + " + Colombia");
                    }
                }

                relevantes.add(noticia);

                if (!alertaRepository.existsByUrlOrigen(noticia.getUrlOrigen())) {
                    nuevasParaGuardar.add(noticia);
                }
            }
        }

        // Priorizar siempre primero las noticias que mencionan explícitamente el municipio en el titular
        relevantes.sort((a, b) -> {
            boolean munA = "LA_ESCUELA_TE_VISITA_UBICACION_CONFIRMADA".equals(a.getTipoInformacion());
            boolean munB = "LA_ESCUELA_TE_VISITA_UBICACION_CONFIRMADA".equals(b.getTipoInformacion());
            if (munA != munB) return munA ? -1 : 1;
            return 0;
        });

        alertaRepository.saveAll(nuevasParaGuardar);

        List<String> nombresMonitoreados = CONFIG_EMPRESAS.stream()
                .map(e -> "LA_ESCUELA_TE_VISITA".equals(e.categoria()) ? e.nombreMostrar() + " (" + e.ubicacion() + ")" : e.nombreMostrar())
                .collect(Collectors.toList());

        Map<String, Object> reporte = new LinkedHashMap<>();
        reporte.put("titulo", "Reporte de Monitoreo: La Escuela Te Visita & Grandes Empresas");
        reporte.put("fuentesAnalizadas", "Google News RSS");
        reporte.put("empresasMonitoreadas", nombresMonitoreados);
        reporte.put("totalNoticiasAnalizadas", noticiasBrutas.size());
        reporte.put("mencionesRelevantesDetectadas", relevantes.size());
        reporte.put("falsosPositivosDescartados", falsosPositivos.size());
        reporte.put("ejemplosFalsosPositivos", falsosPositivos.stream().limit(5).collect(Collectors.toList()));
        reporte.put("alertasRelevantes", relevantes);

        return reporte;
    }

    /**
     * Consulta los 4 pares de [Empresa / 1 Sinónimo] + [Ubicación / 1 Sinónimo] en Google News RSS.
     */
    private List<Alerta> extraerNoticiasRssPorEmpresa(EmpresaConfig emp) {
        List<Alerta> resultado = new ArrayList<>();
        Set<String> titulosVistos = new HashSet<>();
        try {
            List<String> lugares = emp.terminosUbicacion();

            record ParBusqueda(String sinonimoEmpresa, String lugar, String url) {}
            List<ParBusqueda> pares = new ArrayList<>();

            // Consulta de cada par [Empresa + Lugar] asegurando presencia de la empresa y la ubicación
            for (String lugar : lugares) {
                for (String sinonimoEmpresa : emp.aliasBusqueda()) {
                    String qPar = "\"" + sinonimoEmpresa.trim() + "\" \"" + lugar.trim() + "\"";
                    String encPar = java.net.URLEncoder.encode(qPar, java.nio.charset.StandardCharsets.UTF_8);
                    String urlGoogleNews = "https://news.google.com/rss/search?q=" + encPar + "&hl=es-419&gl=CO&ceid=CO:es-419";
                    pares.add(new ParBusqueda(sinonimoEmpresa.trim(), lugar.trim(), urlGoogleNews));
                }
            }

            List<java.util.concurrent.CompletableFuture<Map.Entry<ParBusqueda, List<Alerta>>>> futuros = pares.stream()
                    .map(par -> java.util.concurrent.CompletableFuture.supplyAsync(() ->
                            Map.entry(par, extraerNoticiasRSS(Collections.singletonList(par.url())))
                    ))
                    .collect(Collectors.toList());

            for (java.util.concurrent.CompletableFuture<Map.Entry<ParBusqueda, List<Alerta>>> futuro : futuros) {
                Map.Entry<ParBusqueda, List<Alerta>> entry = futuro.join();
                ParBusqueda par = entry.getKey();
                for (Alerta a : entry.getValue()) {
                    String tNorm = a.getTitulo() != null ? a.getTitulo().trim().toLowerCase() : "";
                    if (!tNorm.isEmpty() && titulosVistos.add(tNorm)) {
                        a.setEmpresaEntidadRelacionada(emp.nombreMostrar());
                        a.setPalabrasClaveDetectadas("Palabras clave: " + par.sinonimoEmpresa() + " + " + par.lugar());
                        resultado.add(a);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error construyendo URLs de Google News RSS para empresa {}: {}", emp.nombreMostrar(), e.getMessage());
        }
        return resultado;
    }

    /**
     * Consulta rápida en Google News RSS para cada empresa cuando se visualiza una categoría completa.
     */
    private List<Alerta> extraerNoticiasRssResumenPorEmpresa(EmpresaConfig emp) {
        List<Alerta> resultado = new ArrayList<>();
        try {
            String aliasPrincipal = emp.aliasBusqueda().get(0).trim();
            String lugarPrincipal = emp.terminosUbicacion().get(0).trim();

            String q = "\"" + aliasPrincipal + "\" \"" + lugarPrincipal + "\"";
            String enc = java.net.URLEncoder.encode(q, java.nio.charset.StandardCharsets.UTF_8);
            String url = "https://news.google.com/rss/search?q=" + enc + "&hl=es-419&gl=CO&ceid=CO:es-419";

            List<Alerta> parcial = extraerNoticiasRSS(Collections.singletonList(url));
            for (Alerta a : parcial) {
                a.setEmpresaEntidadRelacionada(emp.nombreMostrar());
                a.setPalabrasClaveDetectadas("Palabras clave: " + aliasPrincipal + " + " + lugarPrincipal);
                resultado.add(a);
            }
        } catch (Exception e) {
            log.warn("Error en resumen RSS para {}: {}", emp.nombreMostrar(), e.getMessage());
        }
        return resultado;
    }

    private List<Alerta> extraerNoticiasRSS(List<String> feeds) {
        List<Alerta> alertas = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            for (String feedUrl : feeds) {
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
                            String link = decodificarUrlReal(getTagValue("link", eElement));
                            String rawDescripcion = getRawTagValue("description", eElement);
                            String descripcion = rawDescripcion != null
                                    ? rawDescripcion.replaceAll("<[^>]*>", "").replace("&nbsp;", " ").trim()
                                    : "";
                            String sourceTag = getTagValue("source", eElement);
                            if (sourceTag == null || sourceTag.isEmpty()) {
                                sourceTag = getTagValue("News:Source", eElement);
                            }

                            String imageUrl = extraerImagen(eElement, rawDescripcion);

                            Alerta alerta = new Alerta(
                                    Herramienta.EMPRESAS_OBJETIVO,
                                    titulo != null ? titulo : "Sin título",
                                    descripcion,
                                    sourceTag != null && !sourceTag.isEmpty() ? sourceTag : obtenerNombreFuente(feedUrl),
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
                    log.warn("No se pudo leer el RSS: " + feedUrl + " - Error: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error inicializando lector RSS", e);
        }
        return alertas;
    }

    /**
     * Si la URL viene de Bing News (apiclick.aspx?...&url=https%3a%2f%2f...), extrae la URL real del medio.
     */
    private String decodificarUrlReal(String rawLink) {
        if (rawLink == null) return null;
        try {
            if (rawLink.contains("url=")) {
                int idx = rawLink.indexOf("url=") + 4;
                int end = rawLink.indexOf("&", idx);
                String encoded = end > idx ? rawLink.substring(idx, end) : rawLink.substring(idx);
                return java.net.URLDecoder.decode(encoded, java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {}
        return rawLink;
    }

    private EmpresaConfig buscarEmpresaConfigPorNombre(String nombreFiltro) {
        if (nombreFiltro == null) return null;
        String buscado = nombreFiltro.trim().toLowerCase();
        for (EmpresaConfig emp : CONFIG_EMPRESAS) {
            if (emp.nombreMostrar().toLowerCase().contains(buscado) || buscado.contains(emp.nombreMostrar().toLowerCase())) {
                return emp;
            }
            for (String alias : emp.aliasBusqueda()) {
                if (alias.toLowerCase().contains(buscado) || buscado.contains(alias.toLowerCase())) {
                    return emp;
                }
            }
        }
        return null;
    }

    private boolean esNoticiaExcluidaOExtranjera(String texto, EmpresaConfig empresa) {
        for (String exclusion : PALABRAS_EXCLUSION_HOMONIMOS) {
            if (texto.contains(exclusion)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verifica que el texto mencione el alias de la empresa, no contenga palabras de homónimos (reinados, deportes)
     * y además coincida con el contexto industrial/sectorial de esa empresa específica.
     */
    private EmpresaConfig detectarEmpresaConIdentidad(String titulo, String descripcion, EmpresaConfig empresaPrioritaria) {
        String texto = (" " + titulo + " " + descripcion + " ").toLowerCase();

        if (esNoticiaExcluidaOExtranjera(texto, empresaPrioritaria)) {
            return null;
        }

        // Si el usuario tiene seleccionada una empresa en el filtro, verificar primero si el artículo habla de ella
        if (empresaPrioritaria != null && cumpleIdentidadEmpresa(texto, empresaPrioritaria)) {
            return empresaPrioritaria;
        }

        for (EmpresaConfig empresa : CONFIG_EMPRESAS) {
            if (cumpleIdentidadEmpresa(texto, empresa)) {
                return empresa;
            }
        }
        return null;
    }

    private String normalizarTexto(String s) {
        if (s == null) return "";
        String norm = java.text.Normalizer.normalize(s.toLowerCase(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return norm.replace("-", " ");
    }

    private boolean cumpleIdentidadEmpresa(String texto, EmpresaConfig empresa) {
        String textoNorm = normalizarTexto(texto);
        boolean coincideAlias = false;
        for (String alias : empresa.aliasBusqueda()) {
            if (texto.contains(alias.toLowerCase()) || textoNorm.contains(normalizarTexto(alias))) {
                coincideAlias = true;
                break;
            }
        }
        if (!coincideAlias) {
            return false;
        }

        boolean coincideContexto = empresa.contextoIdentidad().isEmpty();
        for (String ctx : empresa.contextoIdentidad()) {
            if (texto.contains(ctx.toLowerCase()) || textoNorm.contains(normalizarTexto(ctx))) {
                coincideContexto = true;
                break;
            }
        }
        return coincideContexto;
    }

    /**
     * Verifica si el contenido de la noticia menciona explícitamente la ubicación geográfica
     * (Tocancipá, Sopó, Cajicá, Canavita, Tibitó, Cundinamarca) o la planta industrial de la empresa.
     */
    private boolean verificaMencionUbicacion(String titulo, String descripcion, EmpresaConfig empresa) {
        if (empresa.terminosUbicacion() == null || empresa.terminosUbicacion().isEmpty()) {
            return false;
        }
        String texto = (" " + titulo + " " + descripcion + " ").toLowerCase();
        String textoNorm = normalizarTexto(texto);
        for (String termUbicacion : empresa.terminosUbicacion()) {
            if (texto.contains(termUbicacion.toLowerCase()) || textoNorm.contains(normalizarTexto(termUbicacion))) {
                return true;
            }
        }
        return false;
    }

    private String obtenerSinonimoMarcaDetectado(String titulo, String descripcion, EmpresaConfig empresa) {
        String texto = (" " + titulo + " " + descripcion + " ").toLowerCase();
        String textoNorm = normalizarTexto(texto);
        List<String> encontrados = new ArrayList<>();
        for (String alias : empresa.aliasBusqueda()) {
            if ((texto.contains(alias.toLowerCase()) || textoNorm.contains(normalizarTexto(alias))) && !encontrados.contains(alias)) {
                encontrados.add(alias);
                if (encontrados.size() >= 2) break;
            }
        }
        return encontrados.isEmpty() ? empresa.nombreMostrar() : String.join(", ", encontrados);
    }

    private String obtenerSinonimoUbicacionDetectado(String titulo, String descripcion, EmpresaConfig empresa) {
        if (empresa.terminosUbicacion() == null || empresa.terminosUbicacion().isEmpty()) {
            return empresa.ubicacion();
        }
        String texto = (" " + titulo + " " + descripcion + " ").toLowerCase();
        String textoNorm = normalizarTexto(texto);
        List<String> encontrados = new ArrayList<>();
        for (String term : empresa.terminosUbicacion()) {
            if ((texto.contains(term.toLowerCase()) || textoNorm.contains(normalizarTexto(term))) && !encontrados.contains(term)) {
                encontrados.add(term);
                if (encontrados.size() >= 2) break;
            }
        }
        return encontrados.isEmpty() ? empresa.ubicacion() : String.join(", ", encontrados);
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
        // Opción 0: <News:Image> (Bing News RSS)
        String bingImg = getTagValue("News:Image", eElement);
        if (bingImg != null && !bingImg.isEmpty()) {
            return bingImg;
        }

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
        if (url.contains("bing.com")) return "Prensa Nacional (Bing News)";
        if (url.contains("portafolio")) return "Portafolio Economía";
        if (url.contains("eltiempo")) return "El Tiempo Economía";
        if (url.contains("semana")) return "Revista Semana";
        return "Google News Colombia";
    }

}
