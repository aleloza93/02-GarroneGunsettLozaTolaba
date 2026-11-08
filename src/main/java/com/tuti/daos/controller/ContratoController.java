package com.tuti.daos.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;
import org.springframework.hateoas.EntityModel;
import org.springframework.web.bind.annotation.*;

import com.tuti.daos.dto.ContratoResponse;
import com.tuti.daos.service.impl.ContratoServiceImpl;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.ProblemDetail;

@RestController
@RequestMapping("/contratos")
@Tag(name = "Contratos", description = "Consulta auxiliar compartida por el equipo.")
public class ContratoController {
    private final ContratoServiceImpl service;

    public ContratoController(ContratoServiceImpl service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un contrato",
            description = "Incluye fechaFin calculada y enlace self. Consultar uno finalizado o eliminado no habilita nuevas operaciones.")
    @ApiResponse(responseCode = "200", description = "Contrato encontrado.")
    @ApiResponse(responseCode = "400", description = "ID con formato no numerico.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "El contrato no existe.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public EntityModel<ContratoResponse> consultar(
            @Parameter(description = "ID del contrato.", example = "9001") @PathVariable Long id) {
        return EntityModel.of(service.consultar(id),
                linkTo(methodOn(ContratoController.class).consultar(id)).withSelfRel());
    }
}

