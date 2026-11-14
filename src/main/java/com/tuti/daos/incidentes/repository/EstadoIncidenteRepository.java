package com.tuti.daos.incidentes.repository;

import com.tuti.daos.incidentes.domain.EstadoIncidente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EstadoIncidenteRepository extends JpaRepository<EstadoIncidente, Long> {

    Optional<EstadoIncidente> findByNombreIgnoreCase(String nombre);
}
