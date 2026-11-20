package com.tuti.daos.service;

import java.util.List;

import com.tuti.daos.dto.CambiarEstadoIncidenteRequest;
import com.tuti.daos.dto.HistorialEstadoResponse;
import com.tuti.daos.dto.IncidenteResponse;
import com.tuti.daos.dto.RegistrarIncidenteRequest;
import com.tuti.daos.utilidades.CategoriaIncidente;
import com.tuti.daos.utilidades.EstadoIncidente;
import com.tuti.daos.utilidades.PrioridadIncidente;

public interface IncidenteService {

    IncidenteResponse registrar(RegistrarIncidenteRequest request);

    IncidenteResponse consultar(Long id);

    List<IncidenteResponse> listar(Long contratoId, EstadoIncidente estado,
                                   CategoriaIncidente categoria, PrioridadIncidente prioridad);

    IncidenteResponse cambiarEstado(Long id, CambiarEstadoIncidenteRequest request);

    List<HistorialEstadoResponse> consultarHistorial(Long id);
}
