package com.tuti.pagos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.*;
import com.tuti.pagos.model.MedioPago;

// Las fechas de negocio se validaran en el servicio con el Clock compartido.
public record RegistrarPagoRequest(
        @NotNull @Positive Long facturaId,
        LocalDate fechaPago,
        @NotNull MedioPago medioPago,
        @NotNull @DecimalMin(value = "0.00", inclusive = false)
        @Digits(integer = 13, fraction = 2) BigDecimal importePagado,
        @NotNull @DecimalMin("0.00") @Digits(integer = 13, fraction = 2)
        BigDecimal interes) {}

