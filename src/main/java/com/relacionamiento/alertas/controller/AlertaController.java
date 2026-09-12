package com.relacionamiento.alertas.controller;

import com.relacionamiento.alertas.domain.EstadoAlerta;
import com.relacionamiento.alertas.domain.Herramienta;
import com.relacionamiento.alertas.domain.NivelRelevancia;
import com.relacionamiento.alertas.dto.request.AlertaCreateDto;
import com.relacionamiento.alertas.dto.request.EstadoUpdateDto;
import com.relacionamiento.alertas.dto.response.AlertaResponseDto;
import com.relacionamiento.alertas.dto.response.ApiResponseDto;
import com.relacionamiento.alertas.dto.response.MetricasDto;
import com.relacionamiento.alertas.service.AlertaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/alertas")
@Tag(name = "1. Gestión de Alertas", description = "Endpoints para consulta, filtrado, creación y cambio de estado de alertas de monitoreo.")
public class AlertaController {

    private final AlertaService alertaService;

    public AlertaController(AlertaService alertaService) {
        this.alertaService = alertaService;
    }

    @GetMapping
    @Operation(summary = "Listado paginado de alertas con filtros dinámicos",
            description = "Permite filtrar por herramienta (EMPRESAS, SECOP_COOPERACION, FONDO_RECUPERACION), nivel de relevancia, estado, rango de fechas y término de búsqueda libre.")
    public ResponseEntity<ApiResponseDto<Page<AlertaResponseDto>>> listarAlertas(
            @Parameter(description = "Herramienta de origen") @RequestParam(required = false) Herramienta herramienta,
            @Parameter(description = "Nivel de relevancia (ALTA, MEDIA, BAJA)") @RequestParam(required = false) NivelRelevancia relevancia,
            @Parameter(description = "Estado de la alerta (NUEVA, REVISADA, DESCARTADA)") @RequestParam(required = false) EstadoAlerta estado,
            @Parameter(description = "Búsqueda por texto en título, descripción o empresa") @RequestParam(required = false) String query,
            @Parameter(description = "Fecha inicial de detección (ISO-8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaDesde,
            @Parameter(description = "Fecha final de detección (ISO-8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "fechaDeteccion") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<AlertaResponseDto> resultado = alertaService.listarAlertas(herramienta, relevancia, estado, query, fechaDesde, fechaHasta, pageable);
        return ResponseEntity.ok(ApiResponseDto.ok("Listado de alertas obtenido exitosamente", resultado));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle de una alerta por ID")
    public ResponseEntity<ApiResponseDto<AlertaResponseDto>> obtenerPorId(@PathVariable Long id) {
        AlertaResponseDto dto = alertaService.obtenerPorId(id);
        return ResponseEntity.ok(ApiResponseDto.ok(dto));
    }

    @PostMapping
    @Operation(summary = "Registrar manualmente una nueva alerta")
    public ResponseEntity<ApiResponseDto<AlertaResponseDto>> crearAlerta(@Valid @RequestBody AlertaCreateDto request) {
        AlertaResponseDto creada = alertaService.crearAlerta(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.ok("Alerta registrada exitosamente", creada));
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Actualizar estado de la alerta (NUEVA, REVISADA, DESCARTADA)")
    public ResponseEntity<ApiResponseDto<AlertaResponseDto>> actualizarEstado(
            @PathVariable Long id,
            @Valid @RequestBody EstadoUpdateDto request) {
        AlertaResponseDto actualizada = alertaService.actualizarEstado(id, request.getEstado());
        return ResponseEntity.ok(ApiResponseDto.ok("Estado de la alerta actualizado a " + request.getEstado(), actualizada));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una alerta por ID")
    public ResponseEntity<ApiResponseDto<Void>> eliminarAlerta(@PathVariable Long id) {
        alertaService.eliminarAlerta(id);
        return ResponseEntity.ok(ApiResponseDto.ok("Alerta eliminada correctamente", null));
    }

    @GetMapping("/metricas")
    @Operation(summary = "Obtener métricas y resumen de alertas para el panel web de control")
    public ResponseEntity<ApiResponseDto<MetricasDto>> obtenerMetricas() {
        MetricasDto metricas = alertaService.obtenerMetricas();
        return ResponseEntity.ok(ApiResponseDto.ok("Métricas de alertas calculadas exitosamente", metricas));
    }
}
