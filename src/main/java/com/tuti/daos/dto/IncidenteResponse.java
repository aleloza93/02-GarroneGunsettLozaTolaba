package com.tuti.daos.dto;

import java.time.LocalDateTime;

import org.springframework.hateoas.server.core.Relation;

import com.tuti.daos.utilidades.CategoriaIncidente;
import com.tuti.daos.utilidades.EstadoIncidente;
import com.tuti.daos.utilidades.PrioridadIncidente;

@Relation(collectionRelation = "incidentes", itemRelation = "incidente")
public record IncidenteResponse(
        Long id,
        Long contratoId,
        String titulo,
        String descripcion,
        CategoriaIncidente categoria,
        LocalDateTime fechaAlta,
        PrioridadIncidente prioridad,
        EstadoIncidente estado) {
}
