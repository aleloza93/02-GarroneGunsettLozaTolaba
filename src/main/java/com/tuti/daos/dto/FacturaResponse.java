
package com.tuti.daos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import com.tuti.daos.entities.Factura;
import com.tuti.daos.utilidades.EstadoFactura;

public record FacturaResponse(

    Long id,
    Long contratoId,
    YearMonth periodo,
    String concepto,
    LocalDate fechaEmision,
    LocalDate fechaVencimiento,
    BigDecimal importe,
    EstadoFactura estado,
    boolean facturacionMensual

) {

    public static FacturaResponse desde(
            Factura factura,
            EstadoFactura estadoActual) {

        return new FacturaResponse(
                factura.getId(),
                factura.getContrato().getId(),
                factura.getPeriodo(),
                factura.getConcepto(),
                factura.getFechaEmision(),
                factura.getFechaVencimiento(),
                factura.getImporte(),
                estadoActual,
                factura.isFacturacionMensual()
        );
    }
}
