package com.relacionamiento.alertas.repository;

import com.relacionamiento.alertas.domain.EstadoAlerta;
import com.relacionamiento.alertas.domain.Herramienta;
import com.relacionamiento.alertas.domain.NivelRelevancia;
import com.relacionamiento.alertas.entity.Alerta;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AlertaSpecification {

    public static Specification<Alerta> conFiltros(
            Herramienta herramienta,
            NivelRelevancia relevancia,
            EstadoAlerta estado,
            String query,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta) {

        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (herramienta != null) {
                predicates.add(cb.equal(root.get("herramienta"), herramienta));
            }

            if (relevancia != null) {
                predicates.add(cb.equal(root.get("nivelRelevancia"), relevancia));
            }

            if (estado != null) {
                predicates.add(cb.equal(root.get("estadoAlerta"), estado));
            }

            if (query != null && !query.trim().isEmpty()) {
                String likePattern = "%" + query.trim().toLowerCase() + "%";
                Predicate tituloMatch = cb.like(cb.lower(root.get("titulo")), likePattern);
                Predicate descMatch = cb.like(cb.lower(root.get("descripcion")), likePattern);
                Predicate entidadMatch = cb.like(cb.lower(root.get("empresaEntidadRelacionada")), likePattern);
                Predicate keywordsMatch = cb.like(cb.lower(root.get("palabrasClaveDetectadas")), likePattern);

                predicates.add(cb.or(tituloMatch, descMatch, entidadMatch, keywordsMatch));
            }

            if (fechaDesde != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fechaDeteccion"), fechaDesde));
            }

            if (fechaHasta != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fechaDeteccion"), fechaHasta));
            }

            criteriaQuery.orderBy(cb.desc(root.get("fechaDeteccion")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
