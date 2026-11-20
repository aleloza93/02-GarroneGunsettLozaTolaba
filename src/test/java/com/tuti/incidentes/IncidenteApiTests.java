package com.tuti.incidentes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;
import com.tuti.daos.Application;
import com.tuti.daos.entities.Contrato;
import com.tuti.daos.entities.Incidente;
import com.tuti.daos.repository.ContratoRepository;
import com.tuti.daos.repository.HistorialEstadoIncidenteRepository;
import com.tuti.daos.repository.IncidenteRepository;
import com.tuti.daos.repository.InformeResolucionRepository;
import com.tuti.daos.utilidades.CategoriaIncidente;
import com.tuti.daos.utilidades.EstadoIncidente;
import com.tuti.daos.utilidades.PrioridadIncidente;

@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IncidenteApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ContratoRepository contratoRepository;

    @Autowired
    private IncidenteRepository incidenteRepository;

    @Autowired
    private HistorialEstadoIncidenteRepository historialRepository;

    @Autowired
    private InformeResolucionRepository informeRepository;

    private Contrato contrato;

    @BeforeEach
    void prepararDatos() {
        informeRepository.deleteAll();
        historialRepository.deleteAll();
        incidenteRepository.deleteAll();
        jdbcTemplate.execute("DELETE FROM contratos");

        // 9001: activo y vigente (2020-01-01 a 2039-12-31). 9002: finalizado.
        jdbcTemplate.execute("""
                INSERT INTO contratos (id, fecha_inicio, duracion_meses, importe_mensual, dia_vencimiento, estado, eliminado)
                VALUES (9001, '2020-01-01', 240, 250000.00, 10, 'ACTIVO', false),
                       (9002, '2020-01-01', 240, 180000.00, 10, 'FINALIZADO', false)
                """);

        contrato = contratoRepository.findById(9001L).orElseThrow();
    }

    // ---------- Casos obligatorios ----------

    @Test
    void caso1_registrarIncidente_creaAbiertoConHistorialInicialYEnlaces() throws Exception {
        mockMvc.perform(post("/incidentes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alta(9001L, "Perdida en bano", "PLOMERIA", "ALTA")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.estado").value("ABIERTO"))
                .andExpect(jsonPath("$.contratoId").value(9001))
                .andExpect(jsonPath("$.fechaAlta").exists())
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.contrato.href").exists())
                .andExpect(jsonPath("$._links.informes.href").exists());

        assertThat(incidenteRepository.count()).isEqualTo(1);
        assertThat(historialRepository.findAll()).singleElement().satisfies(h -> {
            assertThat(h.getEstadoAnterior()).isNull();
            assertThat(h.getEstadoNuevo()).isEqualTo(EstadoIncidente.ABIERTO);
        });
    }

    @Test
    void caso2_consultarPorId_devuelveIncidenteConEnlaces() throws Exception {
        String location = registrarYObtenerLocation("Sin agua caliente");

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Sin agua caliente"))
                .andExpect(jsonPath("$._links.self.href").value(location))
                .andExpect(jsonPath("$._links.historial.href").exists());
    }

    @Test
    void caso3_listarFiltrado_devuelveCoincidenciasYListaVaciaSiNoHay() throws Exception {
        crearIncidente(EstadoIncidente.ABIERTO, CategoriaIncidente.GAS);
        crearIncidente(EstadoIncidente.EN_PROCESO, CategoriaIncidente.ELECTRICIDAD);

        mockMvc.perform(get("/incidentes").param("estado", "EN_PROCESO").param("contratoId", "9001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.incidentes.length()").value(1))
                .andExpect(jsonPath("$._embedded.incidentes[0].categoria").value("ELECTRICIDAD"))
                .andExpect(jsonPath("$._links.self.href").exists());

        mockMvc.perform(get("/incidentes").param("contratoId", "999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.incidentes").isEmpty());
    }

    @Test
    void caso4_datosInvalidos_devuelve400YNoGuardaNada() throws Exception {
        String body = """
                {
                  "contratoId": 9001,
                  "titulo": "",
                  "descripcion": "",
                  "prioridad": "ALTA"
                }
                """;

        mockMvc.perform(post("/incidentes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        assertThat(incidenteRepository.count()).isZero();
        assertThat(historialRepository.count()).isZero();
    }

    @Test
    void caso5_recursoInexistente_devuelve404() throws Exception {
        mockMvc.perform(post("/incidentes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alta(999999L, "Contrato inexistente", "GENERAL", "BAJA")))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/incidentes/999999"))
                .andExpect(status().isNotFound());

        assertThat(incidenteRepository.count()).isZero();
    }

    @Test
    void caso6_transicionNoPermitida_devuelve409YNoModificaHistorial() throws Exception {
        Incidente abierto = crearIncidente(EstadoIncidente.ABIERTO, CategoriaIncidente.GENERAL);

        mockMvc.perform(patch("/incidentes/{id}/estado", abierto.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(estado("REABIERTO")))
                .andExpect(status().isConflict());

        assertThat(incidenteRepository.findById(abierto.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoIncidente.ABIERTO);
        assertThat(historialRepository.count()).isZero();
    }

    // ---------- Reglas adicionales ----------

    @Test
    void contratoNoActivo_devuelve409() throws Exception {
        mockMvc.perform(post("/incidentes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alta(9002L, "Contrato finalizado", "GENERAL", "BAJA")))
                .andExpect(status().isConflict());

        assertThat(incidenteRepository.count()).isZero();
    }

    @Test
    void pasarAResueltoPorPatch_devuelve409() throws Exception {
        Incidente enProceso = crearIncidente(EstadoIncidente.EN_PROCESO, CategoriaIncidente.PLOMERIA);

        mockMvc.perform(patch("/incidentes/{id}/estado", enProceso.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(estado("RESUELTO")))
                .andExpect(status().isConflict());
    }

    @Test
    void incidenteCancelado_noAdmiteNuevasTransiciones() throws Exception {
        Long id = idDesdeLocation(registrarYObtenerLocation("Se cancela"));

        mockMvc.perform(patch("/incidentes/{id}/estado", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(estado("CANCELADO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADO"));

        mockMvc.perform(patch("/incidentes/{id}/estado", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(estado("EN_PROCESO")))
                .andExpect(status().isConflict());
    }

    // ---------- Recorrido integrador de mantenimiento (S03 + S04) ----------

    @Test
    void recorridoMantenimiento_conservaAmbosInformesYElHistorialCompleto() throws Exception {
        Long id = idDesdeLocation(registrarYObtenerLocation("Calefon no enciende"));

        cambiar(id, "EN_PROCESO");
        resolver(id, "Cambio de termocupla");
        cambiar(id, "REABIERTO");
        cambiar(id, "EN_PROCESO");
        resolver(id, "Cambio de valvula de gas");

        // Un tercer informe con el incidente RESUELTO se rechaza y no deja registros parciales.
        mockMvc.perform(post("/informes-resolucion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(informe(id, "Intento duplicado")))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/informes-resolucion").param("incidenteId", id.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.informes.length()").value(2));

        mockMvc.perform(get("/incidentes/{id}/historial", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.historial.length()").value(6))
                .andExpect(jsonPath("$._embedded.historial[0].estadoNuevo").value("ABIERTO"))
                .andExpect(jsonPath("$._embedded.historial[1].estadoNuevo").value("EN_PROCESO"))
                .andExpect(jsonPath("$._embedded.historial[2].estadoNuevo").value("RESUELTO"))
                .andExpect(jsonPath("$._embedded.historial[3].estadoNuevo").value("REABIERTO"))
                .andExpect(jsonPath("$._embedded.historial[4].estadoNuevo").value("EN_PROCESO"))
                .andExpect(jsonPath("$._embedded.historial[5].estadoNuevo").value("RESUELTO"))
                .andExpect(jsonPath("$._links.incidente.href").exists());

        assertThat(informeRepository.count()).isEqualTo(2);
        assertThat(incidenteRepository.findById(id).orElseThrow().getEstado()).isEqualTo(EstadoIncidente.RESUELTO);
    }

    // ---------- Auxiliares ----------

    private String alta(Long contratoId, String titulo, String categoria, String prioridad) {
        return """
                {
                  "contratoId": %d,
                  "titulo": "%s",
                  "descripcion": "Descripcion de prueba",
                  "categoria": "%s",
                  "prioridad": "%s"
                }
                """.formatted(contratoId, titulo, categoria, prioridad);
    }

    private String estado(String estado) {
        return "{ \"estado\": \"" + estado + "\" }";
    }

    private String informe(Long incidenteId, String trabajo) {
        return """
                {
                  "incidenteId": %d,
                  "trabajoRealizado": "%s",
                  "costo": 1500.00,
                  "responsable": "Carlos Gomez"
                }
                """.formatted(incidenteId, trabajo);
    }

    private String registrarYObtenerLocation(String titulo) throws Exception {
        return mockMvc.perform(post("/incidentes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alta(9001L, titulo, "GAS", "MEDIA")))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");
    }

    private Long idDesdeLocation(String location) {
        return Long.valueOf(location.substring(location.lastIndexOf('/') + 1));
    }

    private void cambiar(Long id, String nuevoEstado) throws Exception {
        mockMvc.perform(patch("/incidentes/{id}/estado", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(estado(nuevoEstado)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value(nuevoEstado));
    }

    private void resolver(Long id, String trabajo) throws Exception {
        String respuesta = mockMvc.perform(post("/informes-resolucion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(informe(id, trabajo)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(JsonPath.<Number>read(respuesta, "$.incidenteId").longValue()).isEqualTo(id);
    }

    private Incidente crearIncidente(EstadoIncidente estado, CategoriaIncidente categoria) {
        Incidente incidente = new Incidente();
        incidente.setContrato(contrato);
        incidente.setTitulo("Incidente " + categoria);
        incidente.setDescripcion("Creado directamente para la prueba");
        incidente.setCategoria(categoria);
        incidente.setFechaAlta(LocalDateTime.of(2026, 11, 10, 9, 0));
        incidente.setPrioridad(PrioridadIncidente.MEDIA);
        incidente.setEstado(estado);
        return incidenteRepository.save(incidente);
    }
}
