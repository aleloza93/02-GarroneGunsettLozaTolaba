package com.tuti.daos.incidentes.dto;

import com.tuti.daos.incidentes.domain.HistorialEstadoIncidente;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Cambio de estado registrado en el historial del incidente")
public class HistorialEstadoIncidenteResponseDTO {

    private String estadoAnterior;
    private String estadoNuevo;
    private LocalDateTime fechaHora;

    public static HistorialEstadoIncidenteResponseDTO desde(HistorialEstadoIncidente historial) {
        HistorialEstadoIncidenteResponseDTO dto = new HistorialEstadoIncidenteResponseDTO();
        dto.estadoAnterior = historial.getEstadoAnterior() == null
                ? null
                : historial.getEstadoAnterior().getNombre();
        dto.estadoNuevo = historial.getEstadoNuevo().getNombre();
        dto.fechaHora = historial.getFechaHoraCambio();
        return dto;
    }

    public String getEstadoAnterior() {
        return estadoAnterior;
    }

    public String getEstadoNuevo() {
        return estadoNuevo;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }
}
