package com.tuti.daos.informes.controller;

import com.tuti.daos.informes.domain.InformeResolucion;
import com.tuti.daos.informes.dto.InformeResolucionRequestDTO;
import com.tuti.daos.informes.dto.InformeResolucionResponseDTO;
import com.tuti.daos.informes.service.InformeResolucionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.hateoas.Link;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/informes-resolucion")
@Tag(name = "Informes de resolución", description = "Registro y consulta de resoluciones de incidentes")
public class InformeResolucionController {

    private final InformeResolucionService service;

    public InformeResolucionController(InformeResolucionService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(
            summary = "Registrar un informe y resolver un incidente",
            description = "Solo resuelve incidentes en estado EN_PROCESO."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Informe creado e incidente actualizado"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida", content = @Content(schema = @Schema(implementation = com.tuti.daos.informes.exception.ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Incidente no encontrado", content = @Content(schema = @Schema(implementation = com.tuti.daos.informes.exception.ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "El incidente no está EN_PROCESO", content = @Content(schema = @Schema(implementation = com.tuti.daos.informes.exception.ApiErrorResponse.class)))
    })
    public ResponseEntity<InformeResolucionResponseDTO> crear(
            @Valid @RequestBody InformeResolucionRequestDTO request
    ) {
        InformeResolucion informe = service.crear(request);
        InformeResolucionResponseDTO response = conEnlaces(informe);
        URI location = response.getRequiredLink("self").toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un informe de resolución por su ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Informe encontrado"),
            @ApiResponse(responseCode = "404", description = "Informe no encontrado", content = @Content(schema = @Schema(implementation = com.tuti.daos.informes.exception.ApiErrorResponse.class)))
    })
    public InformeResolucionResponseDTO obtener(@PathVariable Long id) {
        return conEnlaces(service.obtenerPorId(id));
    }

    @GetMapping
    @Operation(summary = "Listar informes de resolución", description = "Opcionalmente filtra por incidenteId.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de informes de resolución"),
            @ApiResponse(responseCode = "400", description = "El parámetro incidenteId tiene formato inválido", content = @Content(schema = @Schema(implementation = com.tuti.daos.informes.exception.ApiErrorResponse.class)))
    })
    public List<InformeResolucionResponseDTO> listar(
            @Parameter(description = "Filtra los informes por el ID del incidente")
            @RequestParam(required = false) Long incidenteId
    ) {
        return service.listar(incidenteId).stream()
                .map(this::conEnlaces)
                .toList();
    }

    private InformeResolucionResponseDTO conEnlaces(InformeResolucion informe) {
        InformeResolucionResponseDTO response = InformeResolucionResponseDTO.desde(informe);
        response.add(linkTo(methodOn(InformeResolucionController.class)
                .obtener(informe.getId())).withSelfRel());

        String incidenteHref = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/incidentes/{id}")
                .buildAndExpand(informe.getIncidente().getId())
                .toUriString();
        response.add(Link.of(incidenteHref, "incidente"));
        return response;
    }
}
