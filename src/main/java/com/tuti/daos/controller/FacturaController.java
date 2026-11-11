package com.tuti.daos.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.tuti.daos.dto.FacturaEstadoRequest;
import com.tuti.daos.dto.FacturaRequest;
import com.tuti.daos.dto.FacturaResponse;
import com.tuti.daos.service.FacturaService;
import com.tuti.daos.utilidades.EstadoFactura;

import org.springframework.hateoas.Link;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/facturas")
public class FacturaController {

    private final FacturaService facturaService;

    public FacturaController(FacturaService facturaService) {
        this.facturaService = facturaService;
    }

    @PostMapping
    public ResponseEntity<FacturaResponse> emitir(@Valid @RequestBody FacturaRequest request) {
        FacturaResponse creada = facturaService.emitir(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creada.getId()).toUri();
        return ResponseEntity.created(location).body(conEnlaces(creada));
    }

    @GetMapping("/{id}")
    public FacturaResponse obtener(@PathVariable Long id) {
        return conEnlaces(facturaService.obtener(id));
    }

    @GetMapping
    public List<FacturaResponse> listar(@RequestParam(required = false) Long contratoId,
                                        @RequestParam(required = false) String periodo,
                                        @RequestParam(required = false) EstadoFactura estado) {
        return facturaService.listar(contratoId, periodo, estado)
                .stream().map(this::conEnlaces).toList();
    }

    @PatchMapping("/{id}/estado")
    public FacturaResponse cambiarEstado(@PathVariable Long id,
                                         @Valid @RequestBody FacturaEstadoRequest request) {
        if (request.getEstado() != EstadoFactura.ANULADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se permite pasar a ANULADA. El estado PAGADA se establece registrando un pago");
        }
        return conEnlaces(facturaService.anular(id));
    }

    private FacturaResponse conEnlaces(FacturaResponse r) {
        r.add(enlace("/facturas", r.getId(), "self"));
        r.add(enlace("/contratos", r.getContratoId(), "contrato"));
        if (r.getPagoId() != null) {
            r.add(enlace("/pagos", r.getPagoId(), "pago"));
        }
        return r;
    }

    private Link enlace(String ruta, Long id, String rel) {
        String href = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(ruta + "/{id}").buildAndExpand(id).toUriString();
        return Link.of(href, rel);
    }
}