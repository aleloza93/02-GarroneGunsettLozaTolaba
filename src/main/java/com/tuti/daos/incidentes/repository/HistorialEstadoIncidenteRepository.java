package com.tuti.daos.incidentes.repository;

import com.tuti.daos.incidentes.domain.HistorialEstadoIncidente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistorialEstadoIncidenteRepository
        extends JpaRepository<HistorialEstadoIncidente, Long> {
}
