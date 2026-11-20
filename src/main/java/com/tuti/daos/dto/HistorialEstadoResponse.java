package com.tuti.daos.dto;

import java.time.LocalDateTime;

import org.springframework.hateoas.server.core.Relation;

import com.tuti.daos.utilidades.EstadoIncidente;

@Relation(collectionRelation = "historial", itemRelation = "cambioEstado")
public record HistorialEstadoResponse(
        Long id,
        EstadoIncidente estadoAnterior,
        EstadoIncidente estadoNuevo,
        LocalDateTime fechaHora) {
}
