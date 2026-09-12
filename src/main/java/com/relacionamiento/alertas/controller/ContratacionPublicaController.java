package com.relacionamiento.alertas.controller;

import com.relacionamiento.alertas.domain.EntidadSector;
import com.relacionamiento.alertas.domain.Herramienta;
import com.relacionamiento.alertas.domain.Sector;
import com.relacionamiento.alertas.service.IngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/contratacion-publica")
@Tag(name = "Prototipo 1: Contratación Pública", description = "Endpoints para la extracción de procesos desde SECOP II")
public class ContratacionPublicaController {

    private final IngestionService ingestionService;

    public ContratacionPublicaController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/ejecutar")
    @Operation(summary = "Ejecutar Ingestión Automática (Cron/Job)", description = "Ejecuta un barrido de SECOP. Ideal para procesos automáticos.")
    public ResponseEntity<Map<String, Object>> ejecutarBarrido(
            @Parameter(description = "Sector (opcional)") @RequestParam(required = false) Sector sector,
            @Parameter(description = "Entidad (opcional)") @RequestParam(required = false) EntidadSector entidad,
            @Parameter(description = "Cuantía mínima") @RequestParam(defaultValue = "50000000") double cuantiaMinima,
            @Parameter(description = "Límite") @RequestParam(defaultValue = "10") int limite
    ) {
        return ResponseEntity.ok(ingestionService.ejecutarIngestion(Herramienta.CONTRATACION_PUBLICA, sector, entidad, cuantiaMinima, limite));
    }

    @GetMapping("/reporte")
    @Operation(summary = "Generar Reporte Dinámico de SECOP II", description = "Consulta en tiempo real SECOP II usando los filtros seleccionados y muestra todos los contratos encontrados.")
    public ResponseEntity<Map<String, Object>> generarReporte(
            @Parameter(description = "Sector a consultar") 
            @RequestParam(required = false) Sector sector,
            
            @Parameter(description = "Entidad específica") 
            @RequestParam(required = false) EntidadSector entidad,
            
            @Parameter(description = "Cuantía mínima del contrato en pesos") 
            @RequestParam(defaultValue = "50000000") double cuantiaMinima,
            
            @Parameter(description = "Límite de procesos a consultar") 
            @RequestParam(defaultValue = "10") int limite
    ) {
        // Validación de coherencia de filtros
        if (sector != null && entidad != null && entidad.getSector() != sector) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "La entidad " + entidad.getNombreReal() + " no pertenece al sector " + sector.getDescripcion()
            ));
        }
        
        return ResponseEntity.ok(ingestionService.generarReporteSecop(sector, entidad, cuantiaMinima, limite));
    }

    @GetMapping("/entidades-por-sector")
    @Operation(summary = "Obtener entidades de un sector", description = "Retorna la lista de entidades públicas que pertenecen a un sector.")
    public ResponseEntity<List<Map<String, String>>> obtenerEntidadesPorSector(
            @Parameter(description = "Sector a consultar") @RequestParam Sector sector) {
        
        List<Map<String, String>> entidades = EntidadSector.obtenerPorSector(sector).stream()
                .map(e -> Map.of("id", e.name(), "nombre", e.getNombreReal()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(entidades);
    }
}
