package com.tuti.daos.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tuti.daos.entities.HistorialEstadoIncidente;
import com.tuti.daos.entities.Incidente;
import com.tuti.daos.utilidades.CategoriaIncidente;
import com.tuti.daos.utilidades.EstadoContrato;
import com.tuti.daos.utilidades.EstadoIncidente;
import com.tuti.daos.utilidades.PrioridadIncidente;

import jakarta.persistence.LockModeType;

/**
 * Repositorio de S03 (gestion de incidentes).
 * Convive con IncidenteResolucionRepository (S04), que usa la misma entidad.
 */
public interface IncidenteRepository extends JpaRepository<Incidente, Long> {

    /**
     * Bloquea la fila del incidente mientras se cambia su estado, igual que hace S04 al resolver,
     * para que un PATCH y una resolucion simultaneos no se pisen.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Incidente i where i.id = :id")
    Optional<Incidente> findByIdParaActualizar(@Param("id") Long id);

    @Query("""
            select i from Incidente i
            where (:contratoId is null or i.contrato.id = :contratoId)
              and (:estado is null or i.estado = :estado)
              and (:categoria is null or i.categoria = :categoria)
              and (:prioridad is null or i.prioridad = :prioridad)
            order by i.fechaAlta, i.id
            """)
    List<Incidente> buscarFiltrados(@Param("contratoId") Long contratoId,
                                    @Param("estado") EstadoIncidente estado,
                                    @Param("categoria") CategoriaIncidente categoria,
                                    @Param("prioridad") PrioridadIncidente prioridad);

    /** Historial del incidente ordenado cronologicamente (el id desempata cambios en el mismo instante). */
    @Query("""
            select h from HistorialEstadoIncidente h
            where h.incidente.id = :incidenteId
            order by h.fechaHora, h.id
            """)
    List<HistorialEstadoIncidente> buscarHistorial(@Param("incidenteId") Long incidenteId);

    /**
     * Datos del contrato necesarios para validar el alta de un incidente.
     * Se leen con una proyeccion para no depender de los getters de la entidad Contrato (S01).
     */
    @Query("""
            select c.estado as estado, c.eliminado as eliminado,
                   c.fechaInicio as fechaInicio, c.duracionMeses as duracionMeses
            from Contrato c
            where c.id = :contratoId
            """)
    Optional<DatosVigenciaContrato> buscarDatosVigenciaContrato(@Param("contratoId") Long contratoId);

    interface DatosVigenciaContrato {
        EstadoContrato getEstado();

        Boolean getEliminado();

        LocalDate getFechaInicio();

        Integer getDuracionMeses();
    }
}
