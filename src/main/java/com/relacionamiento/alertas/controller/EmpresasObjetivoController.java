package com.relacionamiento.alertas.controller;

import com.relacionamiento.alertas.service.EmpresasObjetivoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/empresas-objetivo")
@Tag(name = "Prototipo 2: Empresas Objetivo", description = "Endpoints para el Prototipo de monitoreo de noticias en Google News RSS de empresas objetivo")
public class EmpresasObjetivoController {

    private final EmpresasObjetivoService empresasObjetivoService;

    public EmpresasObjetivoController(EmpresasObjetivoService empresasObjetivoService) {
        this.empresasObjetivoService = empresasObjetivoService;
    }

    @GetMapping("/reporte-prueba")
    @Operation(
            summary = "Generar reporte de monitoreo de empresas (Google News RSS)",
            description = "Extrae noticias desde Google News RSS aplicando búsqueda progresiva de sinónimos de empresa y ubicación."
    )
    public ResponseEntity<Map<String, Object>> generarReporte(
            @RequestParam(required = false) String empresa,
            @RequestParam(required = false) String categoria
    ) {
        return ResponseEntity.ok(empresasObjetivoService.generarReportePrueba(empresa, categoria));
    }
}
