package com.tuti.daos.service;

import com.tuti.daos.entities.Factura;

public interface FacturaPagoService {
    Factura obtenerParaPago(Long id);

    void marcarPagada(Factura factura);
}