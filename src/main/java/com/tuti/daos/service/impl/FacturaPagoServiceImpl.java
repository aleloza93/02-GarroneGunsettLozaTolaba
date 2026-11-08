package com.tuti.daos.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.tuti.daos.common.exception.RecursoNoEncontradoException;
import com.tuti.daos.entities.Factura;
import com.tuti.daos.repository.FacturaPagoRepository;
import com.tuti.daos.service.FacturaPagoService;

/** Esta parte conecta Pagos con Facturas sin hacer llamadas HTTP entre los modulos. */
@Service
// MANDATORY nos obliga a usar la transaccion que ya abrio PagoService.
@Transactional(propagation = Propagation.MANDATORY)
public class FacturaPagoServiceImpl implements FacturaPagoService {
    private final FacturaPagoRepository repository;

    public FacturaPagoServiceImpl(FacturaPagoRepository repository) {
        this.repository = repository;
    }

    @Override
    public Factura obtenerParaPago(Long id) {
        return repository.buscarParaActualizar(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la factura " + id + "."));
    }

    @Override
    public void marcarPagada(Factura factura) {
        //factura.marcarPagada();
        // flush manda el cambio a la base; si despues falla algo, todavia se puede deshacer.
        repository.flush();
    }
}