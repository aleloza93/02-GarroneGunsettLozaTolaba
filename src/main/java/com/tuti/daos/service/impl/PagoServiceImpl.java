package com.tuti.daos.service.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.tuti.daos.common.exception.ConflictoNegocioException;
import com.tuti.daos.common.exception.DatosInvalidosException;
import com.tuti.daos.common.exception.RecursoNoEncontradoException;
import com.tuti.daos.dto.PagoResponse;
import com.tuti.daos.dto.RegistrarPagoRequest;
import com.tuti.daos.entities.Factura;
import com.tuti.daos.entities.Pago;
import com.tuti.daos.repository.PagoRepository;
import com.tuti.daos.service.FacturaPagoService;
import com.tuti.daos.service.PagoService;

@Service
@Validated
@Transactional(readOnly = true)
public class PagoServiceImpl implements PagoService {
    private final PagoRepository repository;
    private final FacturaPagoService facturas; // Usamos la interfaz en lugar de la implementación
    private final Clock clock;

    public PagoServiceImpl(PagoRepository repository, FacturaPagoService facturas, Clock clock) {
        this.repository = repository;
        this.facturas = facturas;
        this.clock = clock;
    }

    // Aca guardamos todo junto: si algo falla, no queda un pago a medias.
    @Override
    @Transactional
    public PagoResponse registrar(@NotNull @Valid RegistrarPagoRequest request) {
        // Bloqueamos la factura para que dos solicitudes no la paguen al mismo tiempo.
        Factura factura = facturas.obtenerParaPago(request.facturaId());
        factura.verificarPagable();
        if (repository.existsByFactura_Id(factura.getId())) {
            throw new ConflictoNegocioException("La factura ya tiene un pago registrado.");
        }

        // Si no mandan fecha usamos hoy; con Clock podemos fijar ese "hoy" en las pruebas.
        LocalDate hoy = LocalDate.now(clock);
        LocalDate fecha = request.fechaPago() == null ? hoy : request.fechaPago();
        if (fecha.isBefore(factura.getFechaEmision()) || fecha.isAfter(hoy)) {
            throw new DatosInvalidosException("La fecha de pago debe estar entre la emision y hoy.");
        }
        // El interes viene en el pedido. compareTo toma 250000 y 250000.00 como el mismo monto.
        if (request.importePagado().compareTo(factura.getImporte().add(request.interes())) != 0) {
            throw new ConflictoNegocioException(
                    "El importe pagado debe ser igual al importe de la factura mas el interes.");
        }

        // Mandamos el INSERT a la base, pero todavia no confirmamos la transaccion.
        Pago pago = repository.saveAndFlush(new Pago(factura, fecha, request.medioPago(),
                request.importePagado(), request.interes()));
        facturas.marcarPagada(factura);
        return PagoResponse.desde(pago);
    }

    @Override
    public PagoResponse consultar(Long id) {
        // Un ID invalido es 400; si es valido pero no existe, devolvemos 404.
        validarId(id);
        return repository.findById(id).map(PagoResponse::desde)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el pago " + id + "."));
    }

    @Override
    public List<PagoResponse> listar(Long facturaId) {
        // El filtro es opcional. Si no hay pago para esa factura, devolvemos una lista vacia.
        if (facturaId != null) {
            validarId(facturaId);
            return repository.findByFactura_Id(facturaId).stream().map(PagoResponse::desde).toList();
        }
        return repository.findAllByOrderByIdAsc().stream().map(PagoResponse::desde).toList();
    }

    private void validarId(Long id) {
        if (id == null || id <= 0) {
            throw new DatosInvalidosException("El identificador debe ser positivo.");
        }
    }
}