package com.tuti.daos.dto;

import com.tuti.daos.utilidades.EstadoIncidente;

import jakarta.validation.constraints.NotNull;

/**
 * Nuevo estado solicitado para un incidente (PATCH /incidentes/{id}/estado).
 * El pase a RESUELTO no se acepta aca: se hace solo con POST /informes-resolucion (S04).
 */
public record CambiarEstadoIncidenteRequest(

        @NotNull(message = "El nuevo estado es obligatorio (EN_PROCESO, CANCELADO o REABIERTO)")
        EstadoIncidente estado) {
}
