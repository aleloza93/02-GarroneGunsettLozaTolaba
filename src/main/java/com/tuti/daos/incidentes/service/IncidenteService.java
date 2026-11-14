package com.tuti.daos.incidentes.service;

import com.tuti.daos.incidentes.domain.Incidente;
import com.tuti.daos.incidentes.dto.IncidenteResponseDTO;
import com.tuti.daos.incidentes.repository.IncidenteRepository;
import com.tuti.daos.informes.exception.RecursoNoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncidenteService {

    private final IncidenteRepository incidenteRepository;

    public IncidenteService(IncidenteRepository incidenteRepository) {
        this.incidenteRepository = incidenteRepository;
    }

    @Transactional(readOnly = true)
    public IncidenteResponseDTO obtenerPorId(Long id) {
        Incidente incidente = incidenteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el incidente con id " + id
                ));
        return IncidenteResponseDTO.desde(incidente);
    }
}
