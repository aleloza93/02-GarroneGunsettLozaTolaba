package com.tuti.daos.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RegistrarInformeRequest(
        @NotNull(message = "El incidenteId es obligatorio.")
        @Positive(message = "El incidenteId debe ser un numero positivo.")
        Long incidenteId,

        @NotBlank(message = "La descripcion del trabajo realizado es obligatoria.")
        @Size(max = 1000, message = "El trabajo realizado no puede superar los 1000 caracteres.")
        String trabajoRealizado,

        @NotNull(message = "El costo es obligatorio (indicar 0 si no corresponde).")
        @DecimalMin(value = "0.00", inclusive = true, message = "El costo no puede ser negativo.")
        @Digits(integer = 13, fraction = 2, message = "El costo admite hasta 13 enteros y 2 decimales.")
        BigDecimal costo,

        @Size(max = 1000, message = "Las observaciones no pueden superar los 1000 caracteres.")
        String observaciones,

        @NotBlank(message = "El responsable o tecnico es obligatorio.")
        @Size(max = 150, message = "El responsable no puede superar los 150 caracteres.")
        String responsable
) {
}
