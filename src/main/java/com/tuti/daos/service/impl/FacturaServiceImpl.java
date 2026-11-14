
package com.tuti.daos.service.impl;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.tuti.daos.common.exception.RecursoNoEncontradoException;

import com.tuti.daos.common.exception.ConflictoNegocioException;
import com.tuti.daos.dto.FacturaRequest;
import com.tuti.daos.dto.FacturaResponse;
import com.tuti.daos.dto.PagoResponse;
import com.tuti.daos.entities.Contrato;
import com.tuti.daos.entities.Factura;
import com.tuti.daos.repository.FacturaRepository;
import com.tuti.daos.service.ContratoService;
import com.tuti.daos.service.FacturaService;
import com.tuti.daos.service.PagoService;
import com.tuti.daos.utilidades.EstadoContrato;
import com.tuti.daos.utilidades.EstadoFactura;


@Service
public class FacturaServiceImpl implements FacturaService {

    private final FacturaRepository facturaRepository;
    private final ContratoService contratoService;
    private final PagoService pagoService;

    public FacturaServiceImpl(FacturaRepository facturaRepository,
                              ContratoService contratoService,
                              @Lazy PagoService pagoService) {
        this.facturaRepository = facturaRepository;
        this.contratoService = contratoService;
        this.pagoService = pagoService;
    }

    @Override
    @Transactional
    public FacturaResponse emitir(FacturaRequest req) {
        Contrato contrato = contratoService.buscarPorId(req.getContratoId());

        if (contrato.isEliminado() || contrato.getEstado() != EstadoContrato.ACTIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El contrato debe estar activo");
        }

        YearMonth periodo = YearMonth.parse(req.getPeriodo());
        LocalDate finContrato = contrato.getFechaInicio()
                .plusMonths(contrato.getDuracionMeses()).minusDays(1);
        if (periodo.atDay(1).isAfter(finContrato)
                || periodo.atEndOfMonth().isBefore(contrato.getFechaInicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El período no tiene ningún día dentro de la vigencia del contrato");
        }

        boolean mensual = req.getImporte() == null;
        if (mensual && facturaRepository.existsByContratoIdAndPeriodoAndMensualTrueAndEstadoNot(
                req.getContratoId(), req.getPeriodo(), EstadoFactura.ANULADA)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe una facturación mensual no anulada para ese contrato y período");
        }

        LocalDate vencimiento = req.getFechaVencimiento() != null
                ? req.getFechaVencimiento()
                : periodo.atDay(Math.min(contrato.getDiaVencimiento(), periodo.lengthOfMonth()));
        LocalDate emision = req.getFechaEmision() != null ? req.getFechaEmision() : LocalDate.now();
        if (emision.isAfter(vencimiento)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha de emisión no puede ser posterior al vencimiento");
        }

        Factura factura = new Factura();
        factura.setContrato(contrato);
        factura.setPeriodo(req.getPeriodo());
        factura.setConcepto(req.getConcepto());
        factura.setFechaEmision(emision);
        factura.setFechaVencimiento(vencimiento);
        factura.setImporte(mensual ? contrato.getImporteMensual() : req.getImporte());
        factura.setEstado(EstadoFactura.PENDIENTE);
        factura.setMensual(mensual);

        return toResponse(facturaRepository.save(factura));
    }

    @Override
    @Transactional(readOnly = true)
    public FacturaResponse obtener(Long id) {
        return toResponse(buscarOFallar(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FacturaResponse> listar(Long contratoId, String periodo, EstadoFactura estado) {
        EstadoFactura estadoBd = estado;
        Boolean vencidas = null;
        if (estado == EstadoFactura.PENDIENTE) {
            vencidas = false;
        } else if (estado == EstadoFactura.VENCIDA) {
            estadoBd = EstadoFactura.PENDIENTE;
            vencidas = true;
        }
        return facturaRepository.buscar(contratoId, periodo, estadoBd, vencidas, LocalDate.now())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FacturaResponse anular(Long id) {
        Factura factura = buscarOFallar(id);
        EstadoFactura actual = estadoEfectivo(factura);
        if (actual != EstadoFactura.PENDIENTE && actual != EstadoFactura.VENCIDA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se pueden anular facturas pendientes o vencidas (estado actual: " + actual + ")");
        }
        factura.setEstado(EstadoFactura.ANULADA);
        return toResponse(facturaRepository.save(factura));
    }

    private Factura buscarOFallar(Long id) {
        return facturaRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "La factura " + id + " no existe"));
    }

    private EstadoFactura estadoEfectivo(Factura f) {
        if (f.getEstado() == EstadoFactura.PENDIENTE && LocalDate.now().isAfter(f.getFechaVencimiento())) {
            return EstadoFactura.VENCIDA;
        }
        return f.getEstado();
    }

    private FacturaResponse toResponse(Factura f) {
        EstadoFactura estado = estadoEfectivo(f);
        Long pagoId = null;
        if (estado == EstadoFactura.PAGADA) {
            List<PagoResponse> pagos = pagoService.buscarIdPorFactura(f.getId());
            if (!pagos.isEmpty()) {
                pagoId = pagos.get(0).id();
            }
        }
        return new FacturaResponse(f.getId(), f.getContrato().getId(), f.getPeriodo(), f.getConcepto(),
                f.getFechaEmision(), f.getFechaVencimiento(), f.getImporte(), estado, f.isMensual(), pagoId);
    }
    
    @Override
    public Factura obtenerParaPago(Long id) {
        Factura factura = facturaRepository.buscarParaActualizar(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la factura " + id + "."));
        // Pendiente y vencida se guardan igual como PENDIENTE; cualquier otro estado no admite pago.
        if (factura.getEstado() != EstadoFactura.PENDIENTE) {
            throw new ConflictoNegocioException(
                    "No se puede registrar un pago: la factura está " + factura.getEstado() + ".");
        }
        return factura;
    }

    @Override
    public void marcarPagada(Factura factura) {
        factura.setEstado(EstadoFactura.PAGADA);
        // flush manda el cambio a la base; si después falla algo, todavía se puede deshacer.
        facturaRepository.flush();
    }
    
    @Override
	public void validarPagable(Factura factura) {
        EstadoFactura actual = estadoEfectivo(factura);
        if (actual != EstadoFactura.PENDIENTE && actual != EstadoFactura.VENCIDA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede registrar un pago: la factura está " + actual);
        }
    }
}
