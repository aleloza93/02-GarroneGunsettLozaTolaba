package com.tuti.daos.incidentes.controller;

import com.tuti.daos.incidentes.dto.IncidenteResponseDTO;
import com.tuti.daos.incidentes.service.IncidenteService;
import com.tuti.daos.informes.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.hateoas.Link;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/incidentes")
@Tag(name = "Incidentes", description = "Consulta de incidentes de mantenimiento")
public class IncidenteController {

    private final IncidenteService service;

    public IncidenteController(IncidenteService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un incidente por su ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Incidente encontrado"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Incidente no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public ResponseEntity<IncidenteResponseDTO> obtener(@PathVariable Long id) {
        IncidenteResponseDTO response = service.obtenerPorId(id);
        response.add(linkTo(methodOn(IncidenteController.class).obtener(id)).withSelfRel());

        String contratoHref = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/contratos/{contratoId}")
                .buildAndExpand(response.getContratoId())
                .toUriString();
        response.add(Link.of(contratoHref, "contrato"));

        String informesHref = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/informes-resolucion")
                .queryParam("incidenteId", id)
                .build()
                .toUriString();
        response.add(Link.of(informesHref, "informes"));

        return ResponseEntity.ok(response);
    }
}
