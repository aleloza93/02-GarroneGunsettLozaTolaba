package com.tuti.daos.service.impl;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuti.daos.common.exception.DatosInvalidosException;
import com.tuti.daos.common.exception.RecursoNoEncontradoException;
import com.tuti.daos.dto.InformeResponse;
import com.tuti.daos.dto.RegistrarInformeRequest;
import com.tuti.daos.entities.Incidente;
import com.tuti.daos.entities.InformeResolucion;
import com.tuti.daos.repository.InformeResolucionRepository;
import com.tuti.daos.service.IncidenteResolucionService;
import com.tuti.daos.service.InformeResolucionService;

@Service
public class InformeResolucionServiceImpl implements InformeResolucionService {

    private final InformeResolucionRepository informeRepository;
    private final IncidenteResolucionService incidenteResolucionService;
    private final Clock clock;

    public InformeResolucionServiceImpl(InformeResolucionRepository informeRepository,
            IncidenteResolucionService incidenteResolucionService,
            Clock clock) {
        this.informeRepository = informeRepository;
        this.incidenteResolucionService = incidenteResolucionService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public InformeResponse registrar(RegistrarInformeRequest request) {
        Incidente incidente = incidenteResolucionService.obtenerParaResolucion(request.incidenteId());

        LocalDateTime fechaResolucion = LocalDateTime.now(clock);

        InformeResolucion informe = new InformeResolucion();
        informe.setIncidente(incidente);
        informe.setTrabajoRealizado(request.trabajoRealizado().trim());
        informe.setFechaResolucion(fechaResolucion);
        informe.setCosto(request.costo());
        informe.setObservaciones(request.observaciones() != null ? request.observaciones().trim() : null);
        informe.setResponsable(request.responsable().trim());

        InformeResolucion guardado = informeRepository.saveAndFlush(informe);

        incidenteResolucionService.marcarResueltoYRegistrarHistorial(incidente, fechaResolucion);

        return aResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public InformeResponse consultar(Long id) {
        if (id == null || id <= 0) {
            throw new DatosInvalidosException("El ID del informe debe ser un numero positivo.");
        }
        InformeResolucion informe = informeRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el informe de resolucion con id " + id));
        return aResponse(informe);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InformeResponse> listar(Long incidenteId, String responsable) {
        if (incidenteId != null && incidenteId <= 0) {
            throw new DatosInvalidosException("El filtro incidenteId debe ser un numero positivo.");
        }
        String filtroResponsable = (responsable != null && !responsable.isBlank())
                ? responsable.trim()
                : null;

        return informeRepository.buscarFiltrados(incidenteId, filtroResponsable)
                .stream()
                .map(this::aResponse)
                .toList();
    }

    private InformeResponse aResponse(InformeResolucion informe) {
        return new InformeResponse(
                informe.getId(),
                informe.getIncidente().getId(),
                informe.getTrabajoRealizado(),
                informe.getFechaResolucion(),
                informe.getCosto(),
                informe.getObservaciones(),
                informe.getResponsable());
    }
}
