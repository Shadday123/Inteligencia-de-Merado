package com.relacionamiento.alertas.controller;

import com.relacionamiento.alertas.domain.Herramienta;
import com.relacionamiento.alertas.service.IngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/contratacion-publica")
@Tag(name = "Prototipo 1: Contratación Pública", description = "Endpoints para el Prototipo v1 de extracción desde SECOP II")
public class ContratacionPublicaController {

    private final IngestionService ingestionService;

    public ContratacionPublicaController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/ejecutar")
    @Operation(summary = "Ejecutar barrido de Contratación Pública", description = "Busca procesos en SECOP II y los guarda como alertas.")
    public ResponseEntity<Map<String, Object>> ejecutarBarrido() {
        return ResponseEntity.ok(ingestionService.ejecutarIngestion(Herramienta.CONTRATACION_PUBLICA));
    }

    @GetMapping("/reporte-prueba")
    @Operation(
            summary = "Generar reporte de prueba (Semana 3)", 
            description = "Conecta con SECOP II, aplica filtros de sector, monto y entidad, previene duplicados y retorna el reporte."
    )
    public ResponseEntity<Map<String, Object>> generarReporte(
            @Parameter(description = "Macro sector para filtrar entidades (ej. 'educacion', 'tecnologia', o 'general')") 
            @RequestParam(defaultValue = "general") String macroSector,
            @Parameter(description = "Cuantía mínima del contrato en pesos") 
            @RequestParam(defaultValue = "50000000") double cuantiaMinima,
            @Parameter(description = "Límite de procesos a consultar") 
            @RequestParam(defaultValue = "10") int limite
    ) {
        return ResponseEntity.ok(ingestionService.generarReportePruebaSecop(macroSector, cuantiaMinima, limite));
    }
}
