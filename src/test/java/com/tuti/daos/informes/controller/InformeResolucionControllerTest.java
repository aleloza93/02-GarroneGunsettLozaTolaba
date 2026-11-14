package com.tuti.daos.informes.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuti.daos.incidentes.domain.CategoriaIncidente;
import com.tuti.daos.incidentes.domain.EstadoIncidente;
import com.tuti.daos.incidentes.domain.HistorialEstadoIncidente;
import com.tuti.daos.incidentes.domain.Incidente;
import com.tuti.daos.incidentes.domain.PrioridadIncidente;
import com.tuti.daos.incidentes.repository.EstadoIncidenteRepository;
import com.tuti.daos.incidentes.repository.HistorialEstadoIncidenteRepository;
import com.tuti.daos.incidentes.repository.IncidenteRepository;
import com.tuti.daos.incidentes.service.EstadoIncidenteInitializer;
import com.tuti.daos.informes.dto.InformeResolucionRequestDTO;
import com.tuti.daos.informes.repository.InformeResolucionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.endsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InformeResolucionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IncidenteRepository incidenteRepository;

    @Autowired
    private EstadoIncidenteRepository estadoRepository;

    @Autowired
    private EstadoIncidenteInitializer estadoInitializer;

    @MockitoSpyBean
    private HistorialEstadoIncidenteRepository historialRepository;

    @Autowired
    private InformeResolucionRepository informeRepository;

    private EstadoIncidente enProceso;
    private EstadoIncidente abierto;

    @BeforeEach
    void prepararDatos() {
        historialRepository.deleteAll();
        informeRepository.deleteAll();
        incidenteRepository.deleteAll();
        estadoRepository.deleteAll();

        abierto = estadoRepository.save(new EstadoIncidente("ABIERTO"));
        enProceso = estadoRepository.save(new EstadoIncidente("EN_PROCESO"));
        estadoRepository.save(new EstadoIncidente("RESUELTO"));
        estadoRepository.save(new EstadoIncidente("CANCELADO"));
    }

    @Test
    void registraLaResolucionActualizaElIncidenteYDevuelveEnlaces() throws Exception {
        Incidente incidente = incidenteEnEstado(enProceso);

        var result = mockMvc.perform(post("/informes-resolucion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(solicitud(incidente.getId()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.incidenteId").value(incidente.getId().intValue()))
                .andExpect(jsonPath("$.fechaHoraResolucion").isNotEmpty())
                .andExpect(jsonPath("$.costo").value(15000.5))
                .andReturn();

        JsonNode respuesta = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        long informeId = respuesta.get("id").asLong();
        assertTrue(respuesta.at("/_links/self/href").asText()
                .endsWith("/informes-resolucion/" + informeId));
        String incidenteHref = respuesta.at("/_links/incidente/href").asText();
        assertTrue(incidenteHref.endsWith("/incidentes/" + incidente.getId()));
        Incidente actualizado = incidenteRepository.findById(incidente.getId()).orElseThrow();
        assertEquals("RESUELTO", estadoRepository.findById(actualizado.getEstado().getId())
                .orElseThrow().getNombre());
        assertEquals(1, informeRepository.count());

        List<HistorialEstadoIncidente> historial = historialRepository.findAll();
        assertEquals(1, historial.size());
        assertEquals("EN_PROCESO", estadoRepository.findById(historial.get(0).getEstadoAnterior().getId())
                .orElseThrow().getNombre());
        assertEquals("RESUELTO", estadoRepository.findById(historial.get(0).getEstadoNuevo().getId())
                .orElseThrow().getNombre());

        var informeConsultado = mockMvc.perform(get("/informes-resolucion/{id}", informeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(informeId))
                .andExpect(jsonPath("$._links.self.href", endsWith("/informes-resolucion/" + informeId)))
                .andExpect(jsonPath("$._links.incidente.href", endsWith("/incidentes/" + incidente.getId())))
                .andReturn();
        verificarEnlaceIncidenteConsultable(
                objectMapper.readTree(informeConsultado.getResponse().getContentAsByteArray())
        );

        mockMvc.perform(get(URI.create(incidenteHref)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(incidente.getId().intValue()))
                .andExpect(jsonPath("$.contratoId").value(incidente.getContratoId()))
                .andExpect(jsonPath("$.estado").value("RESUELTO"))
                .andExpect(jsonPath("$.historialEstados.length()").value(1))
                .andExpect(jsonPath("$._links.self.href", endsWith("/incidentes/" + incidente.getId())))
                .andExpect(jsonPath("$._links.contrato.href", endsWith("/contratos/" + incidente.getContratoId())))
                .andExpect(jsonPath("$._links.informes.href",
                        endsWith("/informes-resolucion?incidenteId=" + incidente.getId())));
    }

    @Test
    void soloPermiteUnaResolucionCuandoDosSolicitudesLleganSimultaneamente() throws Exception {
        Incidente incidente = incidenteEnEstado(enProceso);
        byte[] contenido = objectMapper.writeValueAsBytes(solicitud(incidente.getId()));
        CountDownLatch inicio = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Integer> primeraRespuesta = executor.submit(() -> {
                inicio.await();
                return mockMvc.perform(post("/informes-resolucion")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(contenido))
                        .andReturn()
                        .getResponse()
                        .getStatus();
            });
            Future<Integer> segundaRespuesta = executor.submit(() -> {
                inicio.await();
                return mockMvc.perform(post("/informes-resolucion")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(contenido))
                        .andReturn()
                        .getResponse()
                        .getStatus();
            });
            inicio.countDown();

            List<Integer> estadosHttp = List.of(
                    primeraRespuesta.get(10, TimeUnit.SECONDS),
                    segundaRespuesta.get(10, TimeUnit.SECONDS)
            );
            assertEquals(1, estadosHttp.stream().filter(status -> status == 201).count());
            assertEquals(1, estadosHttp.stream().filter(status -> status == 409).count());
        } finally {
            executor.shutdownNow();
        }

        assertEquals(1, informeRepository.count());
        Incidente resuelto = incidenteRepository.findById(incidente.getId()).orElseThrow();
        assertEquals("RESUELTO", estadoRepository.findById(resuelto.getEstado().getId())
                .orElseThrow().getNombre());

        List<HistorialEstadoIncidente> historial = historialRepository.findAll();
        assertEquals(1, historial.size());
        assertEquals("EN_PROCESO", estadoRepository.findById(historial.get(0).getEstadoAnterior().getId())
                .orElseThrow().getNombre());
        assertEquals("RESUELTO", estadoRepository.findById(historial.get(0).getEstadoNuevo().getId())
                .orElseThrow().getNombre());
    }

    @Test
    void inicializaEstadosEnBaseVaciaYPermiteResolverSinDuplicarlos() throws Exception {
        estadoRepository.deleteAll();
        estadoInitializer.run(new DefaultApplicationArguments());

        EstadoIncidente enProcesoInicializado = estadoRepository.findByNombreIgnoreCase("EN_PROCESO")
                .orElseThrow();
        Incidente incidente = incidenteEnEstado(enProcesoInicializado);
        mockMvc.perform(post("/informes-resolucion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(solicitud(incidente.getId()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.incidenteId").value(incidente.getId().intValue()));

        estadoInitializer.run(new DefaultApplicationArguments());

        List<String> estados = estadoRepository.findAll().stream()
                .map(EstadoIncidente::getNombre)
                .sorted()
                .toList();
        assertEquals(List.of("ABIERTO", "CANCELADO", "EN_PROCESO", "RESUELTO"), estados);
        Incidente resuelto = incidenteRepository.findById(incidente.getId()).orElseThrow();
        assertEquals("RESUELTO", estadoRepository.findById(resuelto.getEstado().getId())
                .orElseThrow().getNombre());
    }

    @Test
    void consultaIncidenteDevuelveSusDatosYElHistorialCronologico() throws Exception {
        Incidente incidente = incidenteEnEstado(enProceso);
        LocalDateTime primerCambio = LocalDateTime.of(2026, 11, 11, 9, 30);
        LocalDateTime segundoCambio = LocalDateTime.of(2026, 11, 12, 14, 30);
        historialRepository.save(new HistorialEstadoIncidente(
                incidente, null, abierto, primerCambio, null
        ));
        historialRepository.save(new HistorialEstadoIncidente(
                incidente, abierto, enProceso, segundoCambio, "Técnico asignado"
        ));

        mockMvc.perform(get("/incidentes/{id}", incidente.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(incidente.getId().intValue()))
                .andExpect(jsonPath("$.contratoId").value(incidente.getContratoId()))
                .andExpect(jsonPath("$.titulo").value("Pérdida de agua"))
                .andExpect(jsonPath("$.descripcion").value("Filtración debajo de la pileta"))
                .andExpect(jsonPath("$.categoria").value("PLOMERIA"))
                .andExpect(jsonPath("$.fechaHoraAlta").isNotEmpty())
                .andExpect(jsonPath("$.prioridad").value("MEDIA"))
                .andExpect(jsonPath("$.estado").value("EN_PROCESO"))
                .andExpect(jsonPath("$.historialEstados.length()").value(2))
                .andExpect(jsonPath("$.historialEstados[0].estadoAnterior").doesNotExist())
                .andExpect(jsonPath("$.historialEstados[0].estadoNuevo").value("ABIERTO"))
                .andExpect(jsonPath("$.historialEstados[0].fechaHora")
                        .value("2026-11-11T09:30:00"))
                .andExpect(jsonPath("$.historialEstados[1].estadoAnterior").value("ABIERTO"))
                .andExpect(jsonPath("$.historialEstados[1].estadoNuevo").value("EN_PROCESO"))
                .andExpect(jsonPath("$.historialEstados[1].fechaHora")
                        .value("2026-11-12T14:30:00"))
                .andExpect(jsonPath("$._links.self.href", endsWith("/incidentes/" + incidente.getId())))
                .andExpect(jsonPath("$._links.contrato.href", endsWith("/contratos/" + incidente.getContratoId())))
                .andExpect(jsonPath("$._links.informes.href",
                        endsWith("/informes-resolucion?incidenteId=" + incidente.getId())));
    }

    @Test
    void devuelve404SiNoExisteElIncidenteConsultado() throws Exception {
        mockMvc.perform(get("/incidentes/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("No existe el incidente con id 999999"));
    }

    @Test
    void listaInformesYFiltraPorIncidente() throws Exception {
        Incidente filtrado = incidenteEnEstado(enProceso);
        Incidente otro = incidenteEnEstado(enProceso);
        crearInforme(filtrado);
        crearInforme(otro);

        var informesFiltrados = mockMvc.perform(get("/informes-resolucion")
                        .param("incidenteId", filtrado.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].incidenteId").value(filtrado.getId().intValue()))
                .andReturn();
        verificarEnlaceIncidenteConsultable(
                objectMapper.readTree(informesFiltrados.getResponse().getContentAsByteArray()).get(0)
        );

        var todosLosInformes = mockMvc.perform(get("/informes-resolucion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andReturn();
        JsonNode informes = objectMapper.readTree(todosLosInformes.getResponse().getContentAsByteArray());
        for (JsonNode informe : informes) {
            verificarEnlaceIncidenteConsultable(informe);
        }
    }

    @Test
    void devuelve400ConErroresDeValidacionDeCampos() throws Exception {
        InformeResolucionRequestDTO invalida = new InformeResolucionRequestDTO();
        invalida.setIncidenteId(1L);
        invalida.setDescripcionTrabajo(" ");
        invalida.setCosto(new BigDecimal("-0.01"));
        invalida.setResponsable("");

        mockMvc.perform(post("/informes-resolucion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(invalida)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.descripcionTrabajo").exists())
                .andExpect(jsonPath("$.fieldErrors.costo").exists())
                .andExpect(jsonPath("$.fieldErrors.responsable").exists());

        mockMvc.perform(post("/informes-resolucion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.incidenteId").exists())
                .andExpect(jsonPath("$.fieldErrors.descripcionTrabajo").exists())
                .andExpect(jsonPath("$.fieldErrors.costo").exists())
                .andExpect(jsonPath("$.fieldErrors.responsable").exists());

        mockMvc.perform(post("/informes-resolucion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"incidenteId\":1,\"descripcionTrabajo\":\"Trabajo\",\"costo\":\"no-es-un-numero\",\"responsable\":\"Técnico\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        assertEquals(0, informeRepository.count());
    }

    @Test
    void devuelve404SiElIncidenteNoExiste() throws Exception {
        mockMvc.perform(post("/informes-resolucion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(solicitud(999_999L))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("No existe el incidente con id 999999"));

        assertEquals(0, informeRepository.count());
    }

    @Test
    void devuelve409SiElIncidenteNoEstaEnProceso() throws Exception {
        Incidente incidente = incidenteEnEstado(abierto);

        mockMvc.perform(post("/informes-resolucion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(solicitud(incidente.getId()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(
                        "El incidente debe estar en estado EN_PROCESO para registrarse como resuelto"
                ));

        assertEquals(0, informeRepository.count());
        assertEquals(abierto.getId(), incidenteRepository.findById(incidente.getId()).orElseThrow()
                .getEstado().getId());
        assertTrue(historialRepository.findAll().isEmpty());
    }

    @Test
    void devuelve500SinDetallesInternosYRevierteSiFallaElRegistroDelHistorial() throws Exception {
        Incidente incidente = incidenteEnEstado(enProceso);
        InformeResolucionRequestDTO request = solicitud(incidente.getId());
        String detalleInterno = "Fallo simulado al guardar historial: detalle interno de base de datos";

        doThrow(new DataIntegrityViolationException(detalleInterno))
                .when(historialRepository).save(any(HistorialEstadoIncidente.class));

        MvcResult respuesta;
        try {
            respuesta = mockMvc.perform(post("/informes-resolucion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsBytes(request)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.error").value("Internal Server Error"))
                    .andExpect(jsonPath("$.message").value("No se pudo guardar la resolución"))
                    .andExpect(jsonPath("$.path").value("/informes-resolucion"))
                    .andReturn();
        } finally {
            reset(historialRepository);
        }

        String cuerpoRespuesta = respuesta.getResponse().getContentAsString();
        assertFalse(cuerpoRespuesta.contains(detalleInterno));
        assertFalse(cuerpoRespuesta.contains("detalle interno de base de datos"));
        assertEquals(0, informeRepository.count());
        assertTrue(historialRepository.findAll().isEmpty());
        assertEquals(enProceso.getId(), incidenteRepository.findById(incidente.getId()).orElseThrow()
                .getEstado().getId());
    }

    @Test
    void devuelve404SiNoExisteElInformeSolicitado() throws Exception {
        mockMvc.perform(get("/informes-resolucion/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("No existe el informe de resolución con id 999999"));
    }

    @Test
    void devuelve400SiElFiltroNoEsNumerico() throws Exception {
        mockMvc.perform(get("/informes-resolucion").param("incidenteId", "no-es-un-numero"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    private InformeResolucionRequestDTO solicitud(Long incidenteId) {
        InformeResolucionRequestDTO request = new InformeResolucionRequestDTO();
        request.setIncidenteId(incidenteId);
        request.setDescripcionTrabajo("Reparación de cañería principal");
        request.setCosto(new BigDecimal("15000.50"));
        request.setObservaciones("Trabajo finalizado en tiempo y forma");
        request.setResponsable("Juan Pérez - Plomero");
        return request;
    }

    private void crearInforme(Incidente incidente) throws Exception {
        mockMvc.perform(post("/informes-resolucion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(solicitud(incidente.getId()))))
                .andExpect(status().isCreated());
        assertNotNull(incidente.getId());
    }

    private Incidente incidenteEnEstado(EstadoIncidente estado) {
        return incidenteRepository.save(new Incidente(
                42L,
                "Pérdida de agua",
                "Filtración debajo de la pileta",
                CategoriaIncidente.PLOMERIA,
                PrioridadIncidente.MEDIA,
                estado
        ));
    }

    private void verificarEnlaceIncidenteConsultable(JsonNode informe) throws Exception {
        JsonNode incidenteHrefNode = informe.at("/_links/incidente/href");
        String incidenteHref = incidenteHrefNode.isMissingNode() ? null : incidenteHrefNode.asText();
        if (incidenteHref == null) {
            for (JsonNode link : informe.path("links")) {
                if ("incidente".equals(link.path("rel").asText())) {
                    incidenteHref = link.path("href").asText();
                    break;
                }
            }
        }
        assertTrue(incidenteHref != null && !incidenteHref.isBlank(),
                "El informe debe incluir el enlace HATEOAS incidente");
        mockMvc.perform(get(URI.create(incidenteHref)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(informe.get("incidenteId").asInt()));
    }
}
