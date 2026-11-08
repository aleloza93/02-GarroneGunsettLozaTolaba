
package com.tuti.daos.service.impl;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.tuti.daos.common.exception.ConflictoNegocioException;
import com.tuti.daos.common.exception.DatosInvalidosException;
import com.tuti.daos.common.exception.RecursoNoEncontradoException;

import com.tuti.daos.dto.EmitirFacturaRequest;
import com.tuti.daos.dto.FacturaResponse;

import com.tuti.daos.entities.Contrato;
import com.tuti.daos.entities.Factura;

import com.tuti.daos.repository.ContratoRepository;
import com.tuti.daos.repository.FacturaRepository;

import com.tuti.daos.service.FacturaPagoService;
import com.tuti.daos.service.FacturaService;

import com.tuti.daos.utilidades.EstadoContrato;
import com.tuti.daos.utilidades.EstadoFactura;

@Service
@Validated
@Transactional(readOnly = true)
public class FacturaServiceImpl implements FacturaService {
    private final FacturaRepository repository;
    private final ContratoRepository contratoRepository;
    private final Clock clock;

    public FacturaServiceImpl(
            FacturaRepository repository,
            ContratoRepository contratoRepository,
            Clock clock) {

        this.repository = repository;
        this.contratoRepository = contratoRepository;
        this.clock = clock;
    }

    // --------------------------------------------------
    // EMITIR FACTURA
    // --------------------------------------------------

    @Override
    @Transactional
    public FacturaResponse emitir(@NotNull @Valid EmitirFacturaRequest request) {

        if (request == null) {
            throw new DatosInvalidosException("Los datos de la factura son obligatorios.");
        }

        validarId(request.contratoId());

        if (request.periodo() == null) {
            throw new DatosInvalidosException("El período facturado es obligatorio.");
        }

        // Recuperamos el contrato con bloqueo para evitar
        // emisiones mensuales duplicadas simultáneas.
        Contrato contrato = contratoRepository
                .findByIdParaFacturar(request.contratoId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el contrato " + request.contratoId() + "."));

        // El contrato debe estar activo y no eliminado.
        validarContrato(contrato);

        // El período debe coincidir al menos en un día
        // con la vigencia del contrato.
        validarPeriodo(contrato, request.periodo());

        LocalDate hoy = LocalDate.now(clock);

        // Si no se indica fecha de emisión, usamos la actual.
        LocalDate fechaEmision = request.fechaEmision() != null
                ? request.fechaEmision()
                : hoy;

        // Si no se indica vencimiento, lo calculamos utilizando
        // el día de vencimiento definido en el contrato.
        LocalDate fechaVencimiento = request.fechaVencimiento() != null
                ? request.fechaVencimiento()
                : calcularVencimiento(contrato, request.periodo());

        // La fecha de emisión no puede ser posterior al vencimiento.
        if (fechaEmision.isAfter(fechaVencimiento)) {
            throw new DatosInvalidosException(
                    "La fecha de emisión no puede ser posterior a la fecha de vencimiento.");
        }

        // Si el importe no fue informado, es facturación mensual.
        // En caso contrario, se trata de una factura adicional.
        boolean facturacionMensual = request.importe() == null;

        BigDecimal importe = facturacionMensual
                ? contrato.getImporteMensual()
                : request.importe();

        if (importe == null || importe.signum() < 0) {
            throw new DatosInvalidosException(
                    "El importe de la factura no puede ser negativo ni nulo.");
        }

        // Solo se controla duplicidad para facturas mensuales.
        // Las facturas adicionales pueden repetirse en un mismo período.
        if (facturacionMensual
                && repository.existsByContrato_IdAndPeriodoAndFacturacionMensualTrueAndEstadoNot(
                        contrato.getId(),
                        request.periodo(),
                        EstadoFactura.ANULADA)) {

            throw new ConflictoNegocioException(
                    "Ya existe una factura mensual no anulada para ese contrato y período.");
        }

        Factura factura = new Factura(
                contrato,
                request.periodo(),
                request.concepto(),
                fechaEmision,
                fechaVencimiento,
                importe,
                EstadoFactura.PENDIENTE,
                facturacionMensual
        );

        Factura guardada = repository.saveAndFlush(factura);

        return convertirResponse(guardada, hoy);
    }

    // --------------------------------------------------
    // CONSULTAR FACTURA
    // --------------------------------------------------

    @Override
    public FacturaResponse consultar(Long id) {

        validarId(id);

        Factura factura = repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la factura " + id + "."));

        return convertirResponse(factura, LocalDate.now(clock));
    }

    // --------------------------------------------------
    // LISTAR Y FILTRAR FACTURAS
    // --------------------------------------------------

    @Override
    public List<FacturaResponse> listar(
            Long contratoId,
            YearMonth periodo,
            EstadoFactura estado) {

        if (contratoId != null) {
            validarId(contratoId);
        }

        LocalDate hoy = LocalDate.now(clock);

        // Recuperamos las facturas ordenadas por identificador.
        // El estado se calcula para poder incluir las vencidas
        // aunque todavía estén almacenadas como pendientes.
        return repository.findAllByOrderByIdAsc()
                .stream()

                .filter(f -> contratoId == null
                        || f.getContrato().getId().equals(contratoId))

                .filter(f -> periodo == null
                        || f.getPeriodo().equals(periodo))

                .filter(f -> estado == null
                        || calcularEstadoActual(f, hoy) == estado)

                .map(f -> convertirResponse(f, hoy))
                .toList();
    }

    // --------------------------------------------------
    // ANULAR FACTURA
    // --------------------------------------------------

    @Override
    @Transactional
    public FacturaResponse anular(Long id) {

        validarId(id);

        // Recuperamos la factura con bloqueo para evitar
        // conflictos con una operación de pago simultánea.
        Factura factura = repository.findByIdParaPago(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la factura " + id + "."));

        LocalDate hoy = LocalDate.now(clock);

        EstadoFactura estadoActual = calcularEstadoActual(factura, hoy);

        // Solo pueden anularse facturas pendientes o vencidas.
        if (estadoActual != EstadoFactura.PENDIENTE
                && estadoActual != EstadoFactura.VENCIDA) {

            throw new ConflictoNegocioException(
                    "Solo se pueden anular facturas pendientes o vencidas.");
        }

        factura.setEstado(EstadoFactura.ANULADA);

        Factura guardada = repository.saveAndFlush(factura);

        return convertirResponse(guardada, hoy);
    }

    // --------------------------------------------------
    // INTEGRACIÓN CON EL SERVICIO DE PAGOS
    // --------------------------------------------------

    // Recupera una factura bloqueando su registro.
    // Solo permite continuar si está pendiente o vencida.
    /*@Override
    @Transactional
    public Factura obtenerParaPago(Long id) {

        validarId(id);

        Factura factura = repository.findByIdParaPago(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la factura " + id + "."));

        LocalDate hoy = LocalDate.now(clock);

        EstadoFactura estadoActual = calcularEstadoActual(factura, hoy);

        if (estadoActual != EstadoFactura.PENDIENTE
                && estadoActual != EstadoFactura.VENCIDA) {

            throw new ConflictoNegocioException(
                    "La factura no se encuentra en condiciones de recibir un pago.");
        }

        // Si venció, actualizamos su estado almacenado.
        if (estadoActual == EstadoFactura.VENCIDA) {
            factura.setEstado(EstadoFactura.VENCIDA);
        }

        return factura;
    }*/

    // Cambia el estado de la factura a pagada.
    // Se ejecuta dentro de la misma transacción que registra el pago.
    /*@Override
    @Transactional
    public void marcarPagada(Factura factura) {

        if (factura == null || factura.getId() == null) {
            throw new DatosInvalidosException(
                    "La factura indicada no es válida.");
        }

        // Recuperamos nuevamente la factura con bloqueo.
        Factura facturaActual = repository.findByIdParaPago(factura.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la factura " + factura.getId() + "."));

        LocalDate hoy = LocalDate.now(clock);

        EstadoFactura estadoActual = calcularEstadoActual(facturaActual, hoy);

        if (estadoActual != EstadoFactura.PENDIENTE
                && estadoActual != EstadoFactura.VENCIDA) {

            throw new ConflictoNegocioException(
                    "Solo se pueden pagar facturas pendientes o vencidas.");
        }

        facturaActual.setEstado(EstadoFactura.PAGADA);

        repository.saveAndFlush(facturaActual);
    }*/

    // --------------------------------------------------
    // MÉTODOS AUXILIARES PRIVADOS
    // --------------------------------------------------

    private void validarContrato(Contrato contrato) {

        if (contrato.getEstado() != EstadoContrato.ACTIVO) {
            throw new ConflictoNegocioException(
                    "No se pueden emitir facturas para un contrato que no está activo.");
        }

        if (contrato.isEliminado()) {
            throw new ConflictoNegocioException(
                    "No se pueden emitir facturas para un contrato eliminado.");
        }
    }

    private void validarPeriodo(Contrato contrato, YearMonth periodo) {

        LocalDate inicioPeriodo = periodo.atDay(1);
        LocalDate finPeriodo = periodo.atEndOfMonth();

        LocalDate inicioContrato = contrato.getFechaInicio();
        LocalDate finContrato = contrato.getFechaFin();

        boolean tieneInterseccion =
                !finPeriodo.isBefore(inicioContrato)
                && !inicioPeriodo.isAfter(finContrato);

        if (!tieneInterseccion) {
            throw new ConflictoNegocioException(
                    "El período facturado no coincide con la vigencia del contrato.");
        }
    }

    private LocalDate calcularVencimiento(
            Contrato contrato,
            YearMonth periodo) {

        Integer diaVencimiento = contrato.getDiaVencimiento();

        if (diaVencimiento == null
                || diaVencimiento < 1
                || diaVencimiento > 31) {

            throw new DatosInvalidosException(
                    "El día de vencimiento del contrato no es válido.");
        }

        // Si el día no existe en el mes, utilizamos su último día.
        int ultimoDia = periodo.lengthOfMonth();

        int dia = Math.min(diaVencimiento, ultimoDia);

        return periodo.atDay(dia);
    }

    private EstadoFactura calcularEstadoActual(
            Factura factura,
            LocalDate hoy) {

        // Los estados pagada y anulada nunca se modifican
        // automáticamente por el vencimiento.
        if (factura.getEstado() == EstadoFactura.PENDIENTE
                && hoy.isAfter(factura.getFechaVencimiento())) {

            return EstadoFactura.VENCIDA;
        }

        return factura.getEstado();
    }

    private FacturaResponse convertirResponse(
            Factura factura,
            LocalDate hoy) {

        EstadoFactura estadoActual = calcularEstadoActual(factura, hoy);

        return FacturaResponse.desde(factura, estadoActual);
    }

    private void validarId(Long id) {

        if (id == null || id <= 0) {
            throw new DatosInvalidosException(
                    "El identificador debe ser un número positivo.");
        }
    }
}
