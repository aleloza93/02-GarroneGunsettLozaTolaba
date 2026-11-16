package com.tuti.informes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

import com.tuti.daos.Application;
import com.tuti.daos.entities.Contrato;
import com.tuti.daos.entities.Incidente;
import com.tuti.daos.repository.ContratoRepository;
import com.tuti.daos.repository.HistorialEstadoIncidenteRepository;
import com.tuti.daos.repository.IncidenteResolucionRepository;
import com.tuti.daos.repository.InformeResolucionRepository;
import com.tuti.daos.utilidades.CategoriaIncidente;
import com.tuti.daos.utilidades.EstadoIncidente;
import com.tuti.daos.utilidades.PrioridadIncidente;

@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InformeResolucionApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ContratoRepository contratoRepository;

    @Autowired
    private IncidenteResolucionRepository incidenteRepository;

    @Autowired
    private HistorialEstadoIncidenteRepository historialRepository;

    @Autowired
    private InformeResolucionRepository informeRepository;

    private Incidente incidenteEnProceso;
    private Incidente incidenteAbierto;

    @BeforeEach
    void prepararDatos() {
        informeRepository.deleteAll();
        historialRepository.deleteAll();
        incidenteRepository.deleteAll();
        jdbcTemplate.execute("DELETE FROM contratos");

        jdbcTemplate.execute("""
                INSERT INTO contratos (id, fecha_inicio, duracion_meses, importe_mensual, dia_vencimiento, estado, eliminado)
                VALUES (9001, '2026-01-01', 24, 250000.00, 10, 'ACTIVO', false)
                """);

        Contrato contrato = contratoRepository.findById(9001L).orElseThrow();

        incidenteEnProceso = new Incidente();
        incidenteEnProceso.setContrato(contrato);
        incidenteEnProceso.setTitulo("Perdida en cocina");
        incidenteEnProceso.setDescripcion("Goteo bajo la bacha");
        incidenteEnProceso.setCategoria(CategoriaIncidente.PLOMERIA);
        incidenteEnProceso.setFechaAlta(LocalDateTime.of(2026, 11, 5, 9, 30));
        incidenteEnProceso.setPrioridad(PrioridadIncidente.ALTA);
        incidenteEnProceso.setEstado(EstadoIncidente.EN_PROCESO);
        incidenteEnProceso = incidenteRepository.save(incidenteEnProceso);

        incidenteAbierto = new Incidente();
        incidenteAbierto.setContrato(contrato);
        incidenteAbierto.setTitulo("Calefon sin piloto");
        incidenteAbierto.setDescripcion("Revision de gas");
        incidenteAbierto.setCategoria(CategoriaIncidente.GAS);
        incidenteAbierto.setFechaAlta(LocalDateTime.of(2026, 11, 7, 15, 0));
        incidenteAbierto.setPrioridad(PrioridadIncidente.MEDIA);
        incidenteAbierto.setEstado(EstadoIncidente.ABIERTO);
        incidenteAbierto = incidenteRepository.save(incidenteAbierto);
    }

    @Test
    void caso1_resolverIncidenteExitoso_creaInformeCambiaEstadoYGrabaHistorial() throws Exception {
        String body = """
                {
                  "incidenteId": %d,
                  "trabajoRealizado": "Cambio de flexible y prueba hidraulica",
                  "costo": 18500.00,
                  "observaciones": "Sin perdidas",
                  "responsable": "Carlos Gomez"
                }
                """.formatted(incidenteEnProceso.getId());

        mockMvc.perform(post("/informes-resolucion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.incidenteId").value(incidenteEnProceso.getId()))
                .andExpect(jsonPath("$.responsable").value("Carlos Gomez"))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.incidente.href").exists());

        Incidente actualizado = incidenteRepository.findById(incidenteEnProceso.getId()).orElseThrow();
        assertThat(actualizado.getEstado()).isEqualTo(EstadoIncidente.RESUELTO);
        assertThat(historialRepository.findAll()).hasSize(1);
    }

    @Test
    void caso2_consultarPorId_devuelveInformeConEnlaces() throws Exception {
        String body = """
                {
                  "incidenteId": %d,
                  "trabajoRealizado": "Ajuste de valvula",
                  "costo": 0,
                  "responsable": "Carlos Gomez"
                }
                """.formatted(incidenteEnProceso.getId());

        String location = mockMvc.perform(post("/informes-resolucion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trabajoRealizado").value("Ajuste de valvula"))
                .andExpect(jsonPath("$._links.self.href").value(location));
    }

    @Test
    void caso3_listarFiltrado_devuelveCoincidenciasYListaVaciaSiNoHay() throws Exception {
        String body = """
                {
                  "incidenteId": %d,
                  "trabajoRealizado": "Reparacion completa",
                  "costo": 5000.00,
                  "responsable": "Juan Perez"
                }
                """.formatted(incidenteEnProceso.getId());

        mockMvc.perform(post("/informes-resolucion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/informes-resolucion").param("responsable", "Juan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.informes.length()").value(1));

        mockMvc.perform(get("/informes-resolucion").param("incidenteId", "999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.informes").isEmpty());
    }

    @Test
    void caso4_datosInvalidos_devuelve400() throws Exception {
        String bodyInvalido = """
                {
                  "incidenteId": %d,
                  "trabajoRealizado": "",
                  "costo": -100,
                  "responsable": ""
                }
                """.formatted(incidenteEnProceso.getId());

        mockMvc.perform(post("/informes-resolucion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyInvalido))
                .andExpect(status().isBadRequest());
    }

    @Test
    void caso5_incidenteInexistente_devuelve404() throws Exception {
        String body = """
                {
                  "incidenteId": 999999,
                  "trabajoRealizado": "Trabajo de prueba",
                  "costo": 0,
                  "responsable": "Carlos Gomez"
                }
                """;

        mockMvc.perform(post("/informes-resolucion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void caso6_incidenteNoEnProceso_devuelve409YNoGuardaInforme() throws Exception {
        String body = """
                {
                  "incidenteId": %d,
                  "trabajoRealizado": "Intento sobre incidente abierto",
                  "costo": 1000,
                  "responsable": "Carlos Gomez"
                }
                """.formatted(incidenteAbierto.getId());

        mockMvc.perform(post("/informes-resolucion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());

        assertThat(informeRepository.count()).isZero();
        assertThat(historialRepository.count()).isZero();
    }
}
