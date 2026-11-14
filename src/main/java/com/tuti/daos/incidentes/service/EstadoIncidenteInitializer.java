package com.tuti.daos.incidentes.service;

import com.tuti.daos.incidentes.domain.EstadoIncidente;
import com.tuti.daos.incidentes.repository.EstadoIncidenteRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class EstadoIncidenteInitializer implements ApplicationRunner {

    private static final List<String> ESTADOS_INICIALES = List.of(
            "ABIERTO",
            "EN_PROCESO",
            "RESUELTO",
            "CANCELADO"
    );

    private final EstadoIncidenteRepository estadoRepository;

    public EstadoIncidenteInitializer(EstadoIncidenteRepository estadoRepository) {
        this.estadoRepository = estadoRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (String nombre : ESTADOS_INICIALES) {
            if (estadoRepository.findByNombreIgnoreCase(nombre).isEmpty()) {
                estadoRepository.save(new EstadoIncidente(nombre));
            }
        }
    }
}
