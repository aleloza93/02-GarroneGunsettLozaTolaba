package com.tuti.pagos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.hateoas.server.core.Relation;
import com.tuti.pagos.model.MedioPago;
import com.tuti.pagos.model.Pago;

@Relation(itemRelation = "pago", collectionRelation = "pagos")
public record PagoResponse(Long id, Long facturaId, LocalDate fechaPago,
        MedioPago medioPago, BigDecimal importePagado, BigDecimal interes) {
    public static PagoResponse desde(Pago pago) {
        // Devolvemos los datos del pago y el ID de factura, no toda la entidad relacionada.
        return new PagoResponse(pago.getId(), pago.getFactura().getId(), pago.getFechaPago(),
                pago.getMedioPago(), pago.getImportePagado(), pago.getInteres());
    }
}
