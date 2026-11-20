package com.tuti.daos.dto;

import com.tuti.daos.utilidades.CategoriaIncidente;
import com.tuti.daos.utilidades.PrioridadIncidente;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrada para registrar un incidente (S03).
 * El estado, la fecha de alta y el historial los administra el sistema.
 */
public record RegistrarIncidenteRequest(

        @NotNull(message = "El contrato es obligatorio")
        @Positive(message = "El identificador de contrato debe ser positivo")
        Long contratoId,

        @NotBlank(message = "El titulo es obligatorio")
        @Size(max = 100, message = "El titulo no puede superar los 100 caracteres")
        String titulo,

        @NotBlank(message = "La descripcion es obligatoria")
        @Size(max = 255, message = "La descripcion no puede superar los 255 caracteres")
        String descripcion,

        @NotNull(message = "La categoria es obligatoria (PLOMERIA, ELECTRICIDAD, GAS o GENERAL)")
        CategoriaIncidente categoria,

        @NotNull(message = "La prioridad es obligatoria (BAJA, MEDIA o ALTA)")
        PrioridadIncidente prioridad) {
}
