package com.tuti.daos.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.List;

import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.core.EmbeddedWrappers;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.tuti.daos.dto.InformeResponse;
import com.tuti.daos.dto.RegistrarInformeRequest;
import com.tuti.daos.service.InformeResolucionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/informes-resolucion")
@Tag(name = "Informes de resolucion", description = "S04 - Resolucion de incidentes y registro de informes.")
public class InformeResolucionController {

    private final InformeResolucionService service;

    public InformeResolucionController(InformeResolucionService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Resolver un incidente y registrar su informe",
            description = "Crea el informe de resolucion, cambia el estado del incidente a RESUELTO y registra la transicion en el historial dentro de una misma transaccion.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RegistrarInformeRequest.class),
                            examples = @ExampleObject(name = "Resolucion con costo", value = """
                                    {
                                      "incidenteId": 9201,
                                      "trabajoRealizado": "Se reemplazo el flexible de agua fria y se probo presion sin perdidas.",
                                      "costo": 18500.00,
                                      "observaciones": "Queda en garantia por 6 meses.",
                                      "responsable": "Carlos Gomez"
                                    }
                                    """))))
    @ApiResponse(responseCode = "201", description = "Informe creado e incidente resuelto.",
            headers = @Header(name = "Location", description = "URL del informe creado",
                    schema = @Schema(type = "string", format = "uri")))
    @ApiResponse(responseCode = "400", description = "Campos faltantes o valores invalidos.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "El incidente no existe.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "El incidente no esta en estado EN_PROCESO.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<EntityModel<InformeResponse>> registrar(
            @Valid @RequestBody RegistrarInformeRequest request) {
        EntityModel<InformeResponse> model = representar(service.registrar(request));
        return ResponseEntity.created(model.getRequiredLink("self").toUri()).body(model);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un informe de resolucion por ID")
    @ApiResponse(responseCode = "200", description = "Informe encontrado, con enlaces self e incidente.")
    @ApiResponse(responseCode = "400", description = "ID invalido.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "El informe no existe.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public EntityModel<InformeResponse> consultar(
            @Parameter(description = "ID del informe de resolucion.", example = "1")
            @PathVariable Long id) {
        return representar(service.consultar(id));
    }

    @GetMapping
    @Operation(summary = "Listar y filtrar informes de resolucion",
            description = "Filtros opcionales por incidenteId y responsable. Sin coincidencias devuelve 200 e informes: [].")
    @ApiResponse(responseCode = "200", description = "Listado HAL; self conserva los filtros aplicados.",
            content = @Content(mediaType = "application/hal+json"))
    @ApiResponse(responseCode = "400", description = "Parametro de filtro invalido.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public CollectionModel<?> listar(
            @Parameter(description = "ID positivo del incidente.", example = "9201")
            @RequestParam(required = false) Long incidenteId,
            @Parameter(description = "Nombre parcial o completo del responsable o tecnico.", example = "Carlos")
            @RequestParam(required = false) String responsable) {

        var informes = service.listar(incidenteId, responsable).stream()
                .map(this::representar)
                .toList();

        CollectionModel<?> model = informes.isEmpty()
                ? CollectionModel.of(List.of(new EmbeddedWrappers(false).emptyCollectionOf(InformeResponse.class)))
                : CollectionModel.of(informes);

        return model.add(Link.of(ServletUriComponentsBuilder.fromCurrentRequest().build().toUriString()).withSelfRel());
    }

    private EntityModel<InformeResponse> representar(InformeResponse informe) {
        String incidenteUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/incidentes/{id}")
                .buildAndExpand(informe.incidenteId())
                .toUriString();

        return EntityModel.of(informe,
                linkTo(methodOn(InformeResolucionController.class).consultar(informe.id())).withSelfRel(),
                Link.of(incidenteUrl).withRel("incidente"));
    }
}
