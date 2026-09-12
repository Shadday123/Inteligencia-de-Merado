package com.relacionamiento.alertas.service;

import com.relacionamiento.alertas.domain.EstadoAlerta;
import com.relacionamiento.alertas.domain.Herramienta;
import com.relacionamiento.alertas.domain.NivelRelevancia;
import com.relacionamiento.alertas.dto.request.AlertaCreateDto;
import com.relacionamiento.alertas.dto.response.AlertaResponseDto;
import com.relacionamiento.alertas.dto.response.MetricasDto;
import com.relacionamiento.alertas.entity.Alerta;
import com.relacionamiento.alertas.exception.ResourceNotFoundException;
import com.relacionamiento.alertas.repository.AlertaRepository;
import com.relacionamiento.alertas.repository.AlertaSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class AlertaService {

    private final AlertaRepository alertaRepository;

    public AlertaService(AlertaRepository alertaRepository) {
        this.alertaRepository = alertaRepository;
    }

    @Transactional
    public AlertaResponseDto crearAlerta(AlertaCreateDto dto) {
        Alerta alerta = new Alerta(
                dto.getHerramienta(),
                dto.getTitulo(),
                dto.getDescripcion(),
                dto.getFuente(),
                dto.getTipoInformacion(),
                dto.getEmpresaEntidadRelacionada(),
                dto.getUrlOrigen(),
                dto.getPalabrasClaveDetectadas(),
                dto.getNivelRelevancia(),
                EstadoAlerta.NUEVA,
                dto.getFechaPublicacion()
        );
        Alerta guardada = alertaRepository.save(alerta);
        return AlertaResponseDto.fromEntity(guardada);
    }

    @Transactional(readOnly = true)
    public AlertaResponseDto obtenerPorId(Long id) {
        Alerta alerta = alertaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alerta no encontrada con ID: " + id));
        return AlertaResponseDto.fromEntity(alerta);
    }

    @Transactional(readOnly = true)
    public Page<AlertaResponseDto> listarAlertas(
            Herramienta herramienta,
            NivelRelevancia relevancia,
            EstadoAlerta estado,
            String query,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            Pageable pageable) {

        Specification<Alerta> spec = AlertaSpecification.conFiltros(
                herramienta, relevancia, estado, query, fechaDesde, fechaHasta
        );

        return alertaRepository.findAll(spec, pageable).map(AlertaResponseDto::fromEntity);
    }

    @Transactional
    public AlertaResponseDto actualizarEstado(Long id, EstadoAlerta nuevoEstado) {
        Alerta alerta = alertaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alerta no encontrada con ID: " + id));
        alerta.setEstadoAlerta(nuevoEstado);
        Alerta guardada = alertaRepository.save(alerta);
        return AlertaResponseDto.fromEntity(guardada);
    }

    @Transactional
    public void eliminarAlerta(Long id) {
        if (!alertaRepository.existsById(id)) {
            throw new ResourceNotFoundException("Alerta no encontrada con ID: " + id);
        }
        alertaRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public MetricasDto obtenerMetricas() {
        long total = alertaRepository.count();
        long nuevas = alertaRepository.countByEstadoAlerta(EstadoAlerta.NUEVA);
        long revisadas = alertaRepository.countByEstadoAlerta(EstadoAlerta.REVISADA);
        long descartadas = alertaRepository.countByEstadoAlerta(EstadoAlerta.DESCARTADA);

        Map<String, Long> porHerramienta = new HashMap<>();
        for (Herramienta h : Herramienta.values()) {
            porHerramienta.put(h.name(), alertaRepository.countByHerramienta(h));
        }

        Map<String, Long> porRelevancia = new HashMap<>();
        for (NivelRelevancia r : NivelRelevancia.values()) {
            porRelevancia.put(r.name(), alertaRepository.countByNivelRelevancia(r));
        }

        return new MetricasDto(total, nuevas, revisadas, descartadas, porHerramienta, porRelevancia);
    }
}
