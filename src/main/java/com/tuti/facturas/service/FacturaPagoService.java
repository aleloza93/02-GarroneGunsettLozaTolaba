package com.tuti.facturas.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.tuti.common.exception.RecursoNoEncontradoException;
import com.tuti.facturas.model.Factura;
import com.tuti.facturas.repository.FacturaPagoRepository;

/** Esta parte conecta Pagos con Facturas sin hacer llamadas HTTP entre los modulos. */
@Service
// MANDATORY nos obliga a usar la transaccion que ya abrio PagoService.
@Transactional(propagation = Propagation.MANDATORY)
public class FacturaPagoService {
    private final FacturaPagoRepository repository;

    public FacturaPagoService(FacturaPagoRepository repository) {
        this.repository = repository;
    }

    public Factura obtenerParaPago(Long id) {
        return repository.buscarParaActualizar(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la factura " + id + "."));
    }

    public void marcarPagada(Factura factura) {
        factura.marcarPagada();
        // flush manda el cambio a la base; si despues falla algo, todavia se puede deshacer.
        repository.flush();
    }
}
