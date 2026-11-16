package com.tuti.daos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "informes", itemRelation = "informe")
public record InformeResponse(
        Long id,
        Long incidenteId,
        String trabajoRealizado,
        LocalDateTime fechaResolucion,
        BigDecimal costo,
        String observaciones,
        String responsable
) {
}
