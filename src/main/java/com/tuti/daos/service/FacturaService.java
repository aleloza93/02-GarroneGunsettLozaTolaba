package com.tuti.daos.service;

import java.time.YearMonth;
import java.util.List;

import com.tuti.daos.dto.EmitirFacturaRequest;
import com.tuti.daos.dto.FacturaResponse;
import com.tuti.daos.utilidades.EstadoFactura;

public interface FacturaService {

    // Emitir una nueva factura.
    FacturaResponse emitir(EmitirFacturaRequest request);

    // Consultar una factura por su identificador.
    FacturaResponse consultar(Long id);

    // Listar facturas aplicando filtros opcionales.
    List<FacturaResponse> listar(
            Long contratoId,
            YearMonth periodo,
            EstadoFactura estado
    );

    // Anular una factura.
    FacturaResponse anular(Long id);
}