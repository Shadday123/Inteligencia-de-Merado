package com.relacionamiento.alertas.controller;

import com.relacionamiento.alertas.service.CooperacionInternacionalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/cooperacion")
@Tag(name = "Prototipo 3: Cooperación y Fondo Post-Terremoto", description = "Endpoints para el Prototipo v1 de oportunidades internacionales y reconstrucción")
public class CooperacionInternacionalController {

    private final CooperacionInternacionalService cooperacionService;

    public CooperacionInternacionalController(CooperacionInternacionalService cooperacionService) {
        this.cooperacionService = cooperacionService;
    }

    @GetMapping("/reporte-prueba")
    @Operation(
            summary = "Generar reporte de prueba de Cooperación ",
            description = "Escanea fuentes de agencias internacionales (BID, CAF, OCHA, APC), aplica filtros de relevancia por convocatorias o fondo de reconstrucción (Fondo Milagro) y genera reporte."
    )
    public ResponseEntity<Map<String, Object>> generarReporte() {
        return ResponseEntity.ok(cooperacionService.generarReportePrueba());
    }
}
