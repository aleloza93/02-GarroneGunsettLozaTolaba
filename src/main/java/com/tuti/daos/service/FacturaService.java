package com.tuti.daos.service;

import java.util.List;

import com.tuti.daos.dto.FacturaRequest;
import com.tuti.daos.dto.FacturaResponse;
import com.tuti.daos.entities.Factura;
import com.tuti.daos.utilidades.EstadoFactura;

public interface FacturaService {
    FacturaResponse emitir(FacturaRequest request);
    FacturaResponse obtener(Long id);
    List<FacturaResponse> listar(Long contratoId, String periodo, EstadoFactura estado);
    FacturaResponse anular(Long id);
    Factura obtenerParaPago(Long facturaId);
    void marcarPagada(Factura facturaId);
    void validarPagable(Factura factura);
}