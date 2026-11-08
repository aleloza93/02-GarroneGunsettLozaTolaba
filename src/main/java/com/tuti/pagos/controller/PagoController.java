package com.tuti.pagos.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;
import jakarta.validation.Valid;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.core.EmbeddedWrappers;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import com.tuti.pagos.dto.PagoResponse;
import com.tuti.pagos.dto.RegistrarPagoRequest;
import com.tuti.pagos.service.PagoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.headers.Header;
import org.springframework.http.ProblemDetail;

@RestController
@RequestMapping("/pagos")
@Tag(name = "Pagos", description = "S02 - Registro y consulta de pagos. Responsable: Alejandro.")
public class PagoController {
    private final PagoService service;

    public PagoController(PagoService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Registrar un pago completo",
            description = "Guarda el pago y marca la factura PAGADA en una misma transaccion. "
                    + "El importe base no cambia. Repetir un pago devuelve 409.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true, content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = RegistrarPagoRequest.class),
                    examples = @ExampleObject(name = "Factura con interes", value = """
                            {"facturaId":9102,"medioPago":"TRANSFERENCIA","importePagado":255000,"interes":5000}
                            """))))
    @ApiResponse(responseCode = "201", description = "Pago creado, con enlaces self y factura.",
            headers = @Header(name = "Location", description = "URL del nuevo pago",
                    schema = @Schema(type = "string", format = "uri")))
    @ApiResponse(responseCode = "400", description = "Campos, formatos, importes o fecha invalidos.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "La factura no existe.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Factura pagada/anulada, pago duplicado o monto distinto del total.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = """
                            {"detail":"La factura debe estar pendiente o vencida.","instance":"/pagos",
                            "status":409,"title":"Conflict","_links":{"self":{"href":"/pagos"}}}
                            """)))
    public ResponseEntity<EntityModel<PagoResponse>> registrar(@Valid @RequestBody RegistrarPagoRequest request) {
        // Las reglas van en el servicio; aca armamos la respuesta 201 y la URL del pago creado.
        EntityModel<PagoResponse> model = representar(service.registrar(request));
        return ResponseEntity.created(model.getRequiredLink("self").toUri()).body(model);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un pago por ID")
    @ApiResponse(responseCode = "200", description = "Pago encontrado, con enlaces self y factura.")
    @ApiResponse(responseCode = "400", description = "ID invalido.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "El pago no existe.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public EntityModel<PagoResponse> consultar(
            @Parameter(description = "ID del pago, no de la factura.", example = "1") @PathVariable Long id) {
        return representar(service.consultar(id));
    }

    @GetMapping
    @Operation(summary = "Listar pagos", description = "Filtro opcional por factura. Sin coincidencias devuelve 200 y pagos: [].")
    @ApiResponse(responseCode = "200", description = "Listado HAL; self conserva el filtro.",
            content = @Content(mediaType = "application/hal+json",
                    examples = @ExampleObject(name = "Sin coincidencias", value = """
                            {"_embedded":{"pagos":[]},"_links":{"self":{"href":"http://localhost:8080/pagos?facturaId=999999"}}}
                            """)))
    @ApiResponse(responseCode = "400", description = "Filtro facturaId invalido.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public CollectionModel<?> listar(
            @Parameter(description = "ID positivo de la factura. Dejar vacio para listar todos.", example = "9101")
            @RequestParam(required = false) Long facturaId) {
        var pagos = service.listar(facturaId).stream().map(this::representar).toList();
        // Sin resultados queremos mostrar "pagos": [], no que desaparezca la coleccion del JSON.
        CollectionModel<?> model = pagos.isEmpty()
                ? CollectionModel.of(java.util.List.of(new EmbeddedWrappers(false).emptyCollectionOf(PagoResponse.class)))
                : CollectionModel.of(pagos);
        // Usamos la URL del pedido para conservar el filtro en el enlace self.
        return model.add(Link.of(ServletUriComponentsBuilder.fromCurrentRequest().build().toUriString()).withSelfRel());
    }

    private EntityModel<PagoResponse> representar(PagoResponse pago) {
        // Dejamos listo el enlace a Facturas; el endpoint de destino lo completa Javier en S01.
        String facturaUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/facturas/{id}").buildAndExpand(pago.facturaId()).toUriString();
        return EntityModel.of(pago,
                linkTo(methodOn(PagoController.class).consultar(pago.id())).withSelfRel(),
                Link.of(facturaUrl).withRel("factura"));
    }
}
