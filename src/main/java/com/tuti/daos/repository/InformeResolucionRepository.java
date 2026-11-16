package com.tuti.daos.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tuti.daos.entities.InformeResolucion;

public interface InformeResolucionRepository extends JpaRepository<InformeResolucion, Long> {

    @Query("""
            SELECT i FROM InformeResolucion i
            WHERE (:incidenteId IS NULL OR i.incidente.id = :incidenteId)
              AND (:responsable IS NULL OR LOWER(i.responsable) LIKE LOWER(CONCAT('%', :responsable, '%')))
            ORDER BY i.id ASC
            """)
    List<InformeResolucion> buscarFiltrados(
            @Param("incidenteId") Long incidenteId,
            @Param("responsable") String responsable);
}
