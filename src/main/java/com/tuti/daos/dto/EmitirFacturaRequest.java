
package com.tuti.daos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record EmitirFacturaRequest(

    @NotNull
    @Positive
    Long contratoId,

    @NotNull
    YearMonth periodo,

    String concepto,

    LocalDate fechaEmision,

    LocalDate fechaVencimiento,

    @DecimalMin("0.00")
    BigDecimal importe

) {
}
