package com.relacionamiento.alertas.controller;

import com.relacionamiento.alertas.service.EmpresasObjetivoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/empresas-objetivo")
@Tag(name = "Prototipo 2: Empresas Objetivo", description = "Endpoints para el Prototipo v1 de monitoreo de noticias (RSS) de empresas objetivo")
public class EmpresasObjetivoController {

    private final EmpresasObjetivoService empresasObjetivoService;

    public EmpresasObjetivoController(EmpresasObjetivoService empresasObjetivoService) {
        this.empresasObjetivoService = empresasObjetivoService;
    }

    @GetMapping("/reporte-prueba")
    @Operation(
            summary = "Generar reporte de prueba (Semana 4)",
            description = "Extrae noticias vía RSS, aplica filtros de relevancia, detecta falsos positivos y genera un reporte con más de 60 menciones."
    )
    public ResponseEntity<Map<String, Object>> generarReporte() {
        return ResponseEntity.ok(empresasObjetivoService.generarReportePrueba());
    }
}
