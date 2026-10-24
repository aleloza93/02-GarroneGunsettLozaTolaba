package com.tuti.contratos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContratoApiTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void datos() {
        jdbc.update("DELETE FROM contratos");
        jdbc.update("""
                INSERT INTO contratos (id, fecha_inicio, duracion_meses, importe_mensual,
                    dia_vencimiento, estado, eliminado)
                VALUES (9001, '2026-01-01', 24, 250000.00, 10, 'ACTIVO', FALSE),
                       (9003, '2026-01-01', 24, 250000.00, 10, 'ACTIVO', TRUE),
                       (9004, '2026-01-31', 1, 320000.00, 31, 'ACTIVO', FALSE)
                """);
    }

    @Test
    void consultaConFechasYEnlaceSelf() throws Exception {
        mvc.perform(get("/contratos/9001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9001))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.fechaFin").value("2027-12-31"))
                .andExpect(jsonPath("$.importeMensual").value(250000.00))
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/contratos/9001"));
    }

    @Test
    void recursoInexistente() throws Exception {
        mvc.perform(get("/contratos/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$._links.self.href").value("/contratos/999999"));
    }

    @Test
    void identificadorMalFormado() throws Exception {
        mvc.perform(get("/contratos/abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void consultaHistoricaNoOcultaEliminados() throws Exception {
        mvc.perform(get("/contratos/9003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eliminado").value(true));
    }

    @Test
    void fechaFinSumaMesesYRestaUnDia() throws Exception {
        mvc.perform(get("/contratos/9004"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechaFin").value("2026-02-27"));
    }
}

