package com.relacionamiento.alertas.controller;

import com.relacionamiento.alertas.domain.Herramienta;
import com.relacionamiento.alertas.dto.response.ApiResponseDto;
import com.relacionamiento.alertas.service.IngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ingestion")
@Tag(name = "2. Módulo de Ingestión y Monitoreo", description = "Disparadores para barrido periódico de fuentes y monitoreo de novedades.")
public class IngestionController {

    private final IngestionService ingestionService;

    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/ejecutar/{herramienta}")
    @Operation(summary = "Ejecutar barrido manual de monitoreo para una herramienta específica",
            description = "Consulta las fuentes correspondientes a la herramienta, ejecuta las reglas de filtrado y guarda las alertas nuevas detectadas en la base de datos.")
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> ejecutarIngestion(@PathVariable Herramienta herramienta) {
        Map<String, Object> res = ingestionService.ejecutarIngestion(herramienta);
        return ResponseEntity.ok(ApiResponseDto.ok("Monitoreo ejecutado exitosamente", res));
    }

    @GetMapping("/secop/reporte-prueba")
    @Operation(summary = "Semana 3: Generar Reporte de Prueba con datos reales de SECOP II",
            description = "Conecta en vivo a la API SODA de datos.gov.co, extrae procesos de contratación pública filtrados por sector (tecnología, ingeniería, software), cuantía mínima y estado publicado, y los guarda como alertas.")
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> generarReportePruebaSecop(
            @Parameter(description = "Cuantía mínima en COP (por defecto 50.000.000)")
            @RequestParam(defaultValue = "50000000") double cuantiaMinima,
            @Parameter(description = "Número máximo de procesos a extraer")
            @RequestParam(defaultValue = "10") int limite) {

        Map<String, Object> reporte = ingestionService.generarReportePruebaSecop(cuantiaMinima, limite);
        return ResponseEntity.ok(ApiResponseDto.ok("Reporte de prueba de SECOP II generado exitosamente", reporte));
    }

    @GetMapping("/estado")
    @Operation(summary = "Consultar el estado y fechas de última ejecución del monitoreo de fuentes")
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> obtenerEstado() {
        Map<String, Object> estado = ingestionService.obtenerEstadoIngestion();
        return ResponseEntity.ok(ApiResponseDto.ok(estado));
    }
}
