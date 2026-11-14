package com.tuti.daos.informes.service;

import com.tuti.daos.incidentes.domain.EstadoIncidente;
import com.tuti.daos.incidentes.domain.HistorialEstadoIncidente;
import com.tuti.daos.incidentes.domain.Incidente;
import com.tuti.daos.incidentes.repository.EstadoIncidenteRepository;
import com.tuti.daos.incidentes.repository.HistorialEstadoIncidenteRepository;
import com.tuti.daos.incidentes.repository.IncidenteRepository;
import com.tuti.daos.informes.domain.InformeResolucion;
import com.tuti.daos.informes.dto.InformeResolucionRequestDTO;
import com.tuti.daos.informes.exception.ConflictoOperacionException;
import com.tuti.daos.informes.exception.RecursoNoEncontradoException;
import com.tuti.daos.informes.repository.InformeResolucionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InformeResolucionService {

    private static final String ESTADO_EN_PROCESO = "EN_PROCESO";
    private static final String ESTADO_RESUELTO = "RESUELTO";

    private final InformeResolucionRepository informeRepository;
    private final IncidenteRepository incidenteRepository;
    private final EstadoIncidenteRepository estadoRepository;
    private final HistorialEstadoIncidenteRepository historialRepository;

    public InformeResolucionService(
            InformeResolucionRepository informeRepository,
            IncidenteRepository incidenteRepository,
            EstadoIncidenteRepository estadoRepository,
            HistorialEstadoIncidenteRepository historialRepository
    ) {
        this.informeRepository = informeRepository;
        this.incidenteRepository = incidenteRepository;
        this.estadoRepository = estadoRepository;
        this.historialRepository = historialRepository;
    }

    @Transactional
    public InformeResolucion crear(InformeResolucionRequestDTO request) {
        Incidente incidente = incidenteRepository.findByIdForUpdate(request.getIncidenteId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el incidente con id " + request.getIncidenteId()
                ));

        EstadoIncidente estadoAnterior = incidente.getEstado();
        if (!ESTADO_EN_PROCESO.equalsIgnoreCase(estadoAnterior.getNombre())) {
            throw new ConflictoOperacionException(
                    "El incidente debe estar en estado EN_PROCESO para registrarse como resuelto"
            );
        }

        EstadoIncidente estadoResuelto = estadoRepository.findByNombreIgnoreCase(ESTADO_RESUELTO)
                .orElseThrow(() -> new IllegalStateException(
                        "No está configurado el estado requerido RESUELTO"
                ));

        LocalDateTime fechaHoraResolucion = LocalDateTime.now();
        InformeResolucion informe = new InformeResolucion(
                incidente,
                request.getDescripcionTrabajo().trim(),
                fechaHoraResolucion,
                request.getCosto(),
                request.getObservaciones(),
                request.getResponsable().trim()
        );

        InformeResolucion informeGuardado = informeRepository.save(informe);
        incidente.setEstado(estadoResuelto);
        incidenteRepository.save(incidente);
        historialRepository.save(new HistorialEstadoIncidente(
                incidente,
                estadoAnterior,
                estadoResuelto,
                fechaHoraResolucion,
                request.getObservaciones()
        ));
        return informeGuardado;
    }

    @Transactional(readOnly = true)
    public InformeResolucion obtenerPorId(Long id) {
        return informeRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el informe de resolución con id " + id
                ));
    }

    @Transactional(readOnly = true)
    public List<InformeResolucion> listar(Long incidenteId) {
        if (incidenteId == null) {
            return informeRepository.findAllByOrderByFechaHoraResolucionDesc();
        }
        return informeRepository.findAllByIncidente_IdOrderByFechaHoraResolucionDesc(incidenteId);
    }
}
