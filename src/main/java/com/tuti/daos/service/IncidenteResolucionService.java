package com.tuti.daos.service;

import java.time.LocalDateTime;

import com.tuti.daos.entities.Incidente;

public interface IncidenteResolucionService {
    Incidente obtenerParaResolucion(Long incidenteId);

    void marcarResueltoYRegistrarHistorial(Incidente incidente, LocalDateTime fechaResolucion);
}
