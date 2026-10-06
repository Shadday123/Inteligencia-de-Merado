package com.relacionamiento.alertas.repository;

import com.relacionamiento.alertas.domain.EstadoAlerta;
import com.relacionamiento.alertas.domain.Herramienta;
import com.relacionamiento.alertas.domain.NivelRelevancia;
import com.relacionamiento.alertas.entity.Alerta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertaRepository extends JpaRepository<Alerta, Long>, JpaSpecificationExecutor<Alerta> {
    long countByHerramienta(Herramienta herramienta);
    long countByEstadoAlerta(EstadoAlerta estadoAlerta);
    long countByNivelRelevancia(NivelRelevancia nivelRelevancia);
    List<Alerta> findTop5ByHerramientaOrderByFechaDeteccionDesc(Herramienta herramienta);
    List<Alerta> findTop5ByOrderByFechaDeteccionDesc();
    boolean existsByUrlOrigen(String urlOrigen);
}
