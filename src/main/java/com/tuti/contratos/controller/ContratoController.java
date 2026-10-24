package com.tuti.contratos.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;
import org.springframework.hateoas.EntityModel;
import org.springframework.web.bind.annotation.*;
import com.tuti.contratos.dto.ContratoResponse;
import com.tuti.contratos.service.ContratoService;

@RestController
@RequestMapping("/contratos")
public class ContratoController {
    private final ContratoService service;

    public ContratoController(ContratoService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public EntityModel<ContratoResponse> consultar(@PathVariable Long id) {
        return EntityModel.of(service.consultar(id),
                linkTo(methodOn(ContratoController.class).consultar(id)).withSelfRel());
    }
}

