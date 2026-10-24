package com.tuti.facturas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.*;

// Propuesta de entrada para S01. No implica que POST /facturas este implementado.
public record EmitirFacturaRequest(
        @NotNull @Positive Long contratoId,
        @NotBlank @Pattern(regexp = "[0-9]{4}-(0[1-9]|1[0-2])",
                message = "debe tener formato AAAA-MM con un mes valido") String periodo,
        String concepto,
        LocalDate fechaEmision,
        LocalDate fechaVencimiento,
        @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 13, fraction = 2)
        BigDecimal importe) {}

