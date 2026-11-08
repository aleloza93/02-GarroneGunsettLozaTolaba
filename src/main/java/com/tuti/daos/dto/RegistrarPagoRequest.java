package com.tuti.daos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.tuti.daos.utilidades.MedioPago;

import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

// Los ejemplos ayudan a probar desde Swagger. Las validaciones siguen en el servicio.
@Schema(description = "Datos para cancelar una factura completa. No se reciben ID ni estado del pago.")
public record RegistrarPagoRequest(
        @Schema(description = "ID de una factura pendiente o vencida, sin pago previo.", example = "9102")
        @NotNull @Positive Long facturaId,
        @Schema(description = "Opcional: si se omite usamos hoy. Entre la emision y hoy, inclusive.",
                type = "string", format = "date")
        LocalDate fechaPago,
        @Schema(description = "Medio usado para pagar.", example = "TRANSFERENCIA")
        @NotNull MedioPago medioPago,
        @Schema(description = "Total abonado: importe base mas interes. Hasta dos decimales.", example = "255000.00")
        @NotNull @DecimalMin(value = "0.00", inclusive = false)
        @Digits(integer = 13, fraction = 2) BigDecimal importePagado,
        @Schema(description = "Obligatorio incluso si es cero. Se informa, no se calcula automaticamente.",
                example = "5000.00")
        @NotNull @DecimalMin("0.00") @Digits(integer = 13, fraction = 2)
        BigDecimal interes) {}

