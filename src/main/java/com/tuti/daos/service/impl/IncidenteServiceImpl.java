package com.tuti.daos.service.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuti.daos.dto.CambiarEstadoIncidenteRequest;
import com.tuti.daos.dto.HistorialEstadoResponse;
import com.tuti.daos.dto.IncidenteResponse;
import com.tuti.daos.dto.RegistrarIncidenteRequest;
import com.tuti.daos.entities.HistorialEstadoIncidente;
import com.tuti.daos.entities.Incidente;
import com.tuti.daos.common.exception.ConflictoNegocioException;
import com.tuti.daos.common.exception.RecursoNoEncontradoException;
import com.tuti.daos.repository.ContratoRepository;
import com.tuti.daos.repository.HistorialEstadoIncidenteRepository;
import com.tuti.daos.repository.IncidenteRepository;
import com.tuti.daos.repository.IncidenteRepository.DatosVigenciaContrato;
import com.tuti.daos.service.IncidenteService;
import com.tuti.daos.utilidades.CategoriaIncidente;
import com.tuti.daos.utilidades.EstadoContrato;
import com.tuti.daos.utilidades.EstadoIncidente;
import com.tuti.daos.utilidades.PrioridadIncidente;

@Service
public class IncidenteServiceImpl implements IncidenteService {

    /**
     * Transiciones que admite S03. EN_PROCESO -> RESUELTO no figura porque la hace S04
     * (POST /informes-resolucion). CANCELADO no admite ninguna.
     */
    private static final Map<EstadoIncidente, Set<EstadoIncidente>> TRANSICIONES = new EnumMap<>(EstadoIncidente.class);

    static {
        TRANSICIONES.put(EstadoIncidente.ABIERTO, EnumSet.of(EstadoIncidente.EN_PROCESO, EstadoIncidente.CANCELADO));
        TRANSICIONES.put(EstadoIncidente.EN_PROCESO, EnumSet.of(EstadoIncidente.CANCELADO));
        TRANSICIONES.put(EstadoIncidente.RESUELTO, EnumSet.of(EstadoIncidente.REABIERTO));
        TRANSICIONES.put(EstadoIncidente.REABIERTO, EnumSet.of(EstadoIncidente.EN_PROCESO));
        TRANSICIONES.put(EstadoIncidente.CANCELADO, EnumSet.noneOf(EstadoIncidente.class));
    }

    private final IncidenteRepository incidenteRepository;
    private final HistorialEstadoIncidenteRepository historialRepository;
    private final ContratoRepository contratoRepository;
    private final Clock clock;

    public IncidenteServiceImpl(IncidenteRepository incidenteRepository,
                                HistorialEstadoIncidenteRepository historialRepository,
                                ContratoRepository contratoRepository,
                                Clock clock) {
        this.incidenteRepository = incidenteRepository;
        this.historialRepository = historialRepository;
        this.contratoRepository = contratoRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public IncidenteResponse registrar(RegistrarIncidenteRequest request) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        validarContrato(request.contratoId(), ahora.toLocalDate());

        Incidente incidente = new Incidente();
        incidente.setContrato(contratoRepository.getReferenceById(request.contratoId()));
        incidente.setTitulo(request.titulo().trim());
        incidente.setDescripcion(request.descripcion().trim());
        incidente.setCategoria(request.categoria());
        incidente.setPrioridad(request.prioridad());
        incidente.setFechaAlta(ahora);
        incidente.setEstado(EstadoIncidente.ABIERTO);
        incidente = incidenteRepository.save(incidente);

        // Primera entrada del historial: sin estado anterior y con la misma fecha/hora del alta.
        historialRepository.save(new HistorialEstadoIncidente(incidente, null, EstadoIncidente.ABIERTO, ahora));

        return aResponse(incidente);
    }

    @Override
    @Transactional(readOnly = true)
    public IncidenteResponse consultar(Long id) {
        return aResponse(buscarIncidente(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<IncidenteResponse> listar(Long contratoId, EstadoIncidente estado,
                                          CategoriaIncidente categoria, PrioridadIncidente prioridad) {
        return incidenteRepository.buscarFiltrados(contratoId, estado, categoria, prioridad)
                .stream()
                .map(this::aResponse)
                .toList();
    }

    @Override
    @Transactional
    public IncidenteResponse cambiarEstado(Long id, CambiarEstadoIncidenteRequest request) {
        Incidente incidente = incidenteRepository.findByIdParaActualizar(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el incidente " + id));

        EstadoIncidente actual = incidente.getEstado();
        EstadoIncidente nuevo = request.estado();

        if (nuevo == EstadoIncidente.RESUELTO) {
            throw new ConflictoNegocioException(
                    "Un incidente solo puede resolverse registrando un informe con POST /informes-resolucion");
        }
        if (!TRANSICIONES.getOrDefault(actual, Set.of()).contains(nuevo)) {
            throw new ConflictoNegocioException(
                    "No se permite pasar el incidente " + id + " de " + actual + " a " + nuevo);
        }

        // Estado e historial dentro de la misma transaccion: si falla uno, no se guarda ninguno.
        LocalDateTime ahora = LocalDateTime.now(clock);
        incidente.setEstado(nuevo);
        historialRepository.save(new HistorialEstadoIncidente(incidente, actual, nuevo, ahora));

        return aResponse(incidente);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistorialEstadoResponse> consultarHistorial(Long id) {
        buscarIncidente(id);
        return incidenteRepository.buscarHistorial(id)
                .stream()
                .map(h -> new HistorialEstadoResponse(h.getId(), h.getEstadoAnterior(), h.getEstadoNuevo(), h.getFechaHora()))
                .toList();
    }

    private Incidente buscarIncidente(Long id) {
        return incidenteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el incidente " + id));
    }

    /**
     * El contrato debe existir, estar activo, no estar eliminado y estar vigente en la fecha de alta.
     * Fin de vigencia = fecha de inicio + duracion en meses - 1 dia (misma regla que S01).
     */
    private void validarContrato(Long contratoId, LocalDate fechaAlta) {
        DatosVigenciaContrato contrato = incidenteRepository.buscarDatosVigenciaContrato(contratoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el contrato " + contratoId));

        if (Boolean.TRUE.equals(contrato.getEliminado())) {
            throw new ConflictoNegocioException("El contrato " + contratoId + " esta eliminado");
        }
        if (contrato.getEstado() != EstadoContrato.ACTIVO) {
            throw new ConflictoNegocioException(
                    "El contrato " + contratoId + " no esta activo (estado " + contrato.getEstado() + ")");
        }
        if (contrato.getFechaInicio() == null || contrato.getDuracionMeses() == null) {
            throw new ConflictoNegocioException("El contrato " + contratoId + " no tiene vigencia definida");
        }

        LocalDate inicio = contrato.getFechaInicio();
        LocalDate fin = inicio.plusMonths(contrato.getDuracionMeses()).minusDays(1);
        if (fechaAlta.isBefore(inicio) || fechaAlta.isAfter(fin)) {
            throw new ConflictoNegocioException("El contrato " + contratoId + " no esta vigente al " + fechaAlta
                    + " (vigencia del " + inicio + " al " + fin + ")");
        }
    }

    private IncidenteResponse aResponse(Incidente i) {
        return new IncidenteResponse(
                i.getId(),
                i.getContrato().getId(),
                i.getTitulo(),
                i.getDescripcion(),
                i.getCategoria(),
                i.getFechaAlta(),
                i.getPrioridad(),
                i.getEstado());
    }
}
