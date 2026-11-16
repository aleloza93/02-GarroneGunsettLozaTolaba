package com.tuti.daos.service.impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.tuti.daos.common.exception.ConflictoNegocioException;
import com.tuti.daos.common.exception.RecursoNoEncontradoException;
import com.tuti.daos.entities.HistorialEstadoIncidente;
import com.tuti.daos.entities.Incidente;
import com.tuti.daos.repository.HistorialEstadoIncidenteRepository;
import com.tuti.daos.repository.IncidenteResolucionRepository;
import com.tuti.daos.service.IncidenteResolucionService;
import com.tuti.daos.utilidades.EstadoIncidente;

@Service
public class IncidenteResolucionServiceImpl implements IncidenteResolucionService {

    private final IncidenteResolucionRepository incidenteRepository;
    private final HistorialEstadoIncidenteRepository historialRepository;

    public IncidenteResolucionServiceImpl(IncidenteResolucionRepository incidenteRepository,
            HistorialEstadoIncidenteRepository historialRepository) {
        this.incidenteRepository = incidenteRepository;
        this.historialRepository = historialRepository;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Incidente obtenerParaResolucion(Long incidenteId) {
        Incidente incidente = incidenteRepository.findByIdParaBloqueo(incidenteId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el incidente con id " + incidenteId));

        if (incidente.getEstado() != EstadoIncidente.EN_PROCESO) {
            throw new ConflictoNegocioException(
                    "Solo se pueden resolver incidentes en estado EN_PROCESO. Estado actual: "
                            + incidente.getEstado());
        }
        return incidente;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void marcarResueltoYRegistrarHistorial(Incidente incidente, LocalDateTime fechaResolucion) {
        EstadoIncidente estadoAnterior = incidente.getEstado();
        incidente.setEstado(EstadoIncidente.RESUELTO);
        incidenteRepository.saveAndFlush(incidente);

        HistorialEstadoIncidente historial = new HistorialEstadoIncidente(
                incidente,
                estadoAnterior,
                EstadoIncidente.RESUELTO,
                fechaResolucion);
        historialRepository.saveAndFlush(historial);
    }
}
