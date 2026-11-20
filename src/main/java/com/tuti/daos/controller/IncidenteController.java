package com.tuti.daos.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.List;

import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.core.EmbeddedWrapper;
import org.springframework.hateoas.server.core.EmbeddedWrappers;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.tuti.daos.dto.CambiarEstadoIncidenteRequest;
import com.tuti.daos.dto.HistorialEstadoResponse;
import com.tuti.daos.dto.IncidenteResponse;
import com.tuti.daos.dto.RegistrarIncidenteRequest;
import com.tuti.daos.service.IncidenteService;
import com.tuti.daos.utilidades.CategoriaIncidente;
import com.tuti.daos.utilidades.EstadoIncidente;
import com.tuti.daos.utilidades.PrioridadIncidente;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/incidentes")
@Tag(name = "Incidentes", description = "S03 - Registro, consulta y seguimiento de incidentes de mantenimiento")
public class IncidenteController {

    private final IncidenteService service;

    public IncidenteController(IncidenteService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Registrar un incidente",
            description = "Crea el incidente en estado ABIERTO y genera la primera entrada de su historial.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Incidente registrado"),
            @ApiResponse(responseCode = "400", description = "Datos faltantes o invalidos"),
            @ApiResponse(responseCode = "404", description = "El contrato no existe"),
            @ApiResponse(responseCode = "409", description = "El contrato no esta activo, esta eliminado o no esta vigente")
    })
    public ResponseEntity<EntityModel<IncidenteResponse>> registrar(@Valid @RequestBody RegistrarIncidenteRequest request) {
        EntityModel<IncidenteResponse> modelo = representar(service.registrar(request));
        return ResponseEntity.created(modelo.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(modelo);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un incidente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Incidente encontrado"),
            @ApiResponse(responseCode = "404", description = "El incidente no existe")
    })
    public EntityModel<IncidenteResponse> consultar(
            @Parameter(description = "Identificador del incidente", example = "9101") @PathVariable Long id) {
        return representar(service.consultar(id));
    }

    @GetMapping
    @Operation(summary = "Listar y filtrar incidentes",
            description = "Todos los filtros son opcionales y se combinan entre si.")
    @ApiResponse(responseCode = "200", description = "Listado (puede estar vacio)")
    public CollectionModel<?> listar(
            @Parameter(example = "9001") @RequestParam(required = false) Long contratoId,
            @RequestParam(required = false) EstadoIncidente estado,
            @RequestParam(required = false) CategoriaIncidente categoria,
            @RequestParam(required = false) PrioridadIncidente prioridad) {

        List<EntityModel<IncidenteResponse>> modelos = service.listar(contratoId, estado, categoria, prioridad)
                .stream()
                .map(this::representar)
                .toList();
        Link self = linkTo(methodOn(IncidenteController.class).listar(contratoId, estado, categoria, prioridad))
                .withSelfRel();

        return coleccion(modelos, IncidenteResponse.class, self);
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Cambiar el estado de un incidente",
            description = "Transiciones permitidas: ABIERTO -> EN_PROCESO o CANCELADO; EN_PROCESO -> CANCELADO; "
                    + "RESUELTO -> REABIERTO; REABIERTO -> EN_PROCESO. "
                    + "Para resolver un incidente se usa POST /informes-resolucion.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado y registrado en el historial"),
            @ApiResponse(responseCode = "400", description = "Estado faltante o invalido"),
            @ApiResponse(responseCode = "404", description = "El incidente no existe"),
            @ApiResponse(responseCode = "409", description = "Transicion de estado no permitida")
    })
    public EntityModel<IncidenteResponse> cambiarEstado(
            @Parameter(description = "Identificador del incidente", example = "9101") @PathVariable Long id,
            @Valid @RequestBody CambiarEstadoIncidenteRequest request) {
        return representar(service.cambiarEstado(id, request));
    }

    @GetMapping("/{id}/historial")
    @Operation(summary = "Consultar el historial de estados",
            description = "Devuelve los cambios de estado ordenados cronologicamente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial del incidente"),
            @ApiResponse(responseCode = "404", description = "El incidente no existe")
    })
    public CollectionModel<?> historial(
            @Parameter(description = "Identificador del incidente", example = "9101") @PathVariable Long id) {

        List<EntityModel<HistorialEstadoResponse>> modelos = service.consultarHistorial(id)
                .stream()
                .map(EntityModel::of)
                .toList();
        Link self = linkTo(methodOn(IncidenteController.class).historial(id)).withSelfRel();
        Link incidente = linkTo(methodOn(IncidenteController.class).consultar(id)).withRel("incidente");

        return coleccion(modelos, HistorialEstadoResponse.class, self, incidente);
    }

    EntityModel<IncidenteResponse> representar(IncidenteResponse incidente) {
        return EntityModel.of(incidente,
                linkTo(methodOn(IncidenteController.class).consultar(incidente.id())).withSelfRel(),
                linkContrato(incidente.contratoId()),
                linkTo(methodOn(InformeResolucionController.class).listar(incidente.id(), null)).withRel("informes"),
                linkTo(methodOn(IncidenteController.class).historial(incidente.id())).withRel("historial"));
    }

    /**
     * Enlace a GET /contratos/{id}. Si en el proyecto existe un ContratoController,
     * se puede reemplazar por linkTo(methodOn(ContratoController.class).consultar(id)).
     */
    private Link linkContrato(Long contratoId) {
        String href = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/contratos/{id}")
                .buildAndExpand(contratoId)
                .toUriString();
        return Link.of(href, "contrato");
    }

    /** Con lista vacia igual devuelve "_embedded": { "<relacion>": [] }, como en S04. */
    private CollectionModel<?> coleccion(List<?> modelos, Class<?> tipo, Link... links) {
        if (modelos.isEmpty()) {
            EmbeddedWrapper vacio = new EmbeddedWrappers(false).emptyCollectionOf(tipo);
            return CollectionModel.of(List.of(vacio), links);
        }
        return CollectionModel.of(modelos, links);
    }
}
