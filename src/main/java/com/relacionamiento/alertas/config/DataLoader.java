package com.relacionamiento.alertas.config;

import com.relacionamiento.alertas.domain.EstadoAlerta;
import com.relacionamiento.alertas.domain.Herramienta;
import com.relacionamiento.alertas.domain.NivelRelevancia;
import com.relacionamiento.alertas.entity.Alerta;
import com.relacionamiento.alertas.repository.AlertaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);
    private final AlertaRepository alertaRepository;

    public DataLoader(AlertaRepository alertaRepository) {
        this.alertaRepository = alertaRepository;
    }

    @Override
    public void run(String... args) {
        if (alertaRepository.count() == 0) {
            log.info("Inicializando datos semilla (Seed Data) para las tres herramientas...");

            LocalDateTime now = LocalDateTime.now();

            List<Alerta> iniciales = List.of(
                    // Herramienta 1: Empresas Objetivo
                    new Alerta(
                            Herramienta.EMPRESAS_OBJETIVO,
                            "Bancolombia anuncia expansión de fondos para startups y Fintech",
                            "La entidad bancaria destinará $200.000 millones de pesos a líneas de crédito especiales e inversión de impacto para empresas de tecnología.",
                            "Portafolio",
                            "Inversión / Fintech",
                            "Bancolombia",
                            "https://www.portafolio.co/negocios/bancolombia-lineas-fintech-2026",
                            "Bancolombia, Fintech, startups, inversión, tecnología",
                            NivelRelevancia.ALTA,
                            EstadoAlerta.NUEVA,
                            now.minusHours(4)
                    ),
                    new Alerta(
                            Herramienta.EMPRESAS_OBJETIVO,
                            "ISA suscribe acuerdo estratégico para proyectos de interconexión regional",
                            "Interconexión Eléctrica S.A. firma memorando para desarrollar líneas de transmisión de energía limpia en el nororiente del país.",
                            "El Tiempo Economía",
                            "Nuevos Proyectos",
                            "Interconexión Eléctrica S.A. (ISA)",
                            "https://www.eltiempo.com/economia/empresas/isa-proyectos-transmision",
                            "ISA, energía, infraestructura, proyectos, inversión",
                            NivelRelevancia.MEDIA,
                            EstadoAlerta.REVISADA,
                            now.minusDays(1)
                    ),

                    // Herramienta 2: SECOP II y Cooperación Internacional
                    new Alerta(
                            Herramienta.CONTRATACION_PUBLICA,
                            "Convocatoria SECOP II: Consultoría Estratégica para Gestión de Datos",
                            "DNP abre proceso de selección abreviada para estructuración del modelo nacional de analítica de datos en programas sociales. Presupuesto: $4.500M COP.",
                            "SECOP II (datos.gov.co)",
                            "Selección Abreviada",
                            "Departamento Nacional de Planeación (DNP)",
                            "https://colombiacompra.gov.co/secop-ii/proceso/SA-089-2026",
                            "SECOP II, DNP, consultoría, analítica de datos, tecnología",
                            NivelRelevancia.ALTA,
                            EstadoAlerta.NUEVA,
                            now.minusHours(6)
                    ),
                    new Alerta(
                            Herramienta.CONTRATACION_PUBLICA,
                            "CAF lanza programa de apoyo para fortalecimiento institucional",
                            "Línea de crédito blanda y recursos de cooperación técnica para programas de relacionamiento público-privado en Colombia.",
                            "CAF - Banco de Desarrollo de América Latina",
                            "Cooperación Técnica",
                            "CAF / APC Colombia",
                            "https://www.caf.com/es/convocatorias/colombia-relacionamiento",
                            "CAF, cooperación técnica, financiamiento, fondos Colombia",
                            NivelRelevancia.ALTA,
                            EstadoAlerta.NUEVA,
                            now.minusDays(2)
                    ),

                    // Herramienta 3: Fondo de Recuperación Post-Terremoto
                    new Alerta(
                            Herramienta.COOPERACION_INTERNACIONAL,
                            "Fondo Milagro: Desembolso de recursos para rehabilitación de acueductos",
                            "El Ministerio de Vivienda y UNGRD destinan primer paquete financiero de $45.000 millones para reconstrucción de infraestructura básica afectada por el sismo.",
                            "UNGRD / Presidencia",
                            "Recursos de Reconstrucción",
                            "Ministerio de Vivienda / UNGRD",
                            "https://portal.ungrd.gov.co/boletines/fondo-milagro-acueductos",
                            "Fondo Milagro, reconstrucción, terremoto, agua potable, UNGRD, vivienda",
                            NivelRelevancia.ALTA,
                            EstadoAlerta.NUEVA,
                            now.minusHours(2)
                    ),
                    new Alerta(
                            Herramienta.COOPERACION_INTERNACIONAL,
                            "Banco Mundial aprueba fondo de contingencia por USD 150M para mitigación de desastres",
                            "Línea CAT DDO activada para apoyar la etapa de recuperación y reconstrucción tras eventos sísmicos en territorio colombiano.",
                            "Banco Mundial",
                            "Financiamiento Multilateral",
                            "Banco Mundial / Ministerio de Hacienda",
                            "https://bancomundial.org/es/news/press-release/colombia-cat-ddo-desastres",
                            "Banco Mundial, terremoto, CAT DDO, donaciones, financiamiento",
                            NivelRelevancia.ALTA,
                            EstadoAlerta.REVISADA,
                            now.minusDays(3)
                    )
            );

            alertaRepository.saveAll(iniciales);
            log.info("Se cargaron {} alertas semilla exitosamente.", iniciales.size());
        }
    }
}
