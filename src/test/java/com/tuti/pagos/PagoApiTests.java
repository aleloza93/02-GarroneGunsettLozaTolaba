package com.tuti.pagos;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import com.tuti.common.exception.ConflictoNegocioException;
import com.tuti.facturas.model.Factura;
import com.tuti.facturas.service.FacturaPagoService;
import com.tuti.pagos.dto.RegistrarPagoRequest;
import com.tuti.pagos.model.MedioPago;
import com.tuti.pagos.service.PagoService;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:pagos-test;MODE=MySQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PagoApiTests.RelojFijo.class)
class PagoApiTests {
    // Solo para una instancia MySQL EFIMERA de pruebas, nunca la base del TP.
    // El modo habitual sigue usando H2. Ver docs/S02-PAGOS.md antes de activarlo.
    @DynamicPropertySource
    static void mysqlTemporal(DynamicPropertyRegistry registry) {
        if (Boolean.getBoolean("s02.mysql")) {
            registry.add("spring.datasource.url",
                    () -> "jdbc:mysql://127.0.0.1:13307/hogarya_s02_test");
            registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
            registry.add("spring.datasource.username", () -> "root");
            registry.add("spring.datasource.password", () -> "");
        }
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PagoService service;
    @MockitoSpyBean FacturaPagoService facturas;

    @TestConfiguration
    static class RelojFijo {
        @Bean
        @Primary
        Clock relojPrueba() {
            return Clock.fixed(Instant.parse("2026-11-07T15:00:00Z"),
                    ZoneId.of("America/Argentina/Buenos_Aires"));
        }
    }

    @BeforeEach
    void datos() {
        jdbc.update("DELETE FROM pagos");
        jdbc.update("DELETE FROM facturas");
        jdbc.update("DELETE FROM contratos");
        jdbc.update("""
                INSERT INTO contratos (id, fecha_inicio, duracion_meses, importe_mensual,
                    dia_vencimiento, estado, eliminado)
                VALUES (9001, '2026-01-01', 24, 250000.00, 10, 'ACTIVO', FALSE)
                """);
        jdbc.update("""
                INSERT INTO facturas (id, contrato_id, periodo, tipo, concepto,
                    fecha_emision, fecha_vencimiento, importe, estado, version)
                VALUES
                (9101, 9001, '2026-11', 'MENSUAL', 'Pendiente', '2026-11-01', '2026-11-10', 250000, 'PENDIENTE', 0),
                (9102, 9001, '2026-10', 'MENSUAL', 'Vencida calculada', '2026-10-01', '2026-10-10', 250000, 'PENDIENTE', 0),
                (9103, 9001, '2026-09', 'MENSUAL', 'Anulada', '2026-09-01', '2026-09-10', 250000, 'ANULADA', 0),
                (9104, 9001, '2026-08', 'MENSUAL', 'Pagada', '2026-08-01', '2026-08-10', 250000, 'PAGADA', 0),
                (9105, 9001, '2026-07', 'MENSUAL', 'Vencida persistida', '2026-07-01', '2026-07-10', 250000, 'VENCIDA', 0)
                """);
        jdbc.update("""
                INSERT INTO pagos (id, factura_id, fecha_pago, medio_pago, importe_pagado, interes)
                VALUES (9204, 9104, '2026-08-10', 'EFECTIVO', 250000, 0)
                """);
    }

    @Test
    void registraPagoCompletoConFechaActualYEnlaces() throws Exception {
        mvc.perform(post("/pagos").contentType("application/json").content(cuerpo(9101, "250000", "0")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.facturaId").value(9101))
                .andExpect(jsonPath("$.fechaPago").value("2026-11-07"))
                .andExpect(jsonPath("$.importePagado").value(250000))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.factura.href").value("http://localhost/facturas/9101"));
        assertEquals("PAGADA", estado(9101));
        assertEquals(1, cantidad(9101));
        assertEquals(0, new BigDecimal("250000").compareTo(jdbc.queryForObject(
                "SELECT importe FROM facturas WHERE id=9101", BigDecimal.class)));
    }

    @Test
    void consultaPorId() throws Exception {
        mvc.perform(get("/pagos/9204")).andExpect(status().isOk())
                .andExpect(jsonPath("$.facturaId").value(9104))
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/pagos/9204"))
                .andExpect(jsonPath("$._links.factura.href").value("http://localhost/facturas/9104"));
    }

    @Test
    void filtraPorFacturaYConservaElFiltroEnSelf() throws Exception {
        mvc.perform(get("/pagos?facturaId=9104")).andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.pagos.length()").value(1))
                .andExpect(jsonPath("$._embedded.pagos[0].id").value(9204))
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/pagos?facturaId=9104"));
    }

    @Test
    void listaSinFiltro() throws Exception {
        mvc.perform(get("/pagos")).andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.pagos[0].id").value(9204));
    }

    @Test
    void filtroSinCoincidenciasDevuelveColeccionVacia() throws Exception {
        mvc.perform(get("/pagos?facturaId=999999")).andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.pagos").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/pagos/0", "/pagos/-1", "/pagos/abc", "/pagos?facturaId=0", "/pagos?facturaId=abc"})
    void identificadoresInvalidos(String url) throws Exception {
        mvc.perform(get(url)).andExpect(status().isBadRequest());
    }

    @Test
    void pagoInexistente() throws Exception {
        mvc.perform(get("/pagos/999999")).andExpect(status().isNotFound());
    }

    @Test
    void facturaInexistente() throws Exception {
        mvc.perform(post("/pagos").contentType("application/json").content(cuerpo(999999, "250000", "0")))
                .andExpect(status().isNotFound());
        assertEquals(0, cantidad(999999));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"facturaId\":9101}", "{bad json",
            "{\"facturaId\":9101,\"medioPago\":\"CHEQUE\",\"importePagado\":250000,\"interes\":0}",
            "{\"facturaId\":9101,\"medioPago\":\"EFECTIVO\",\"importePagado\":250000}",
            "{\"facturaId\":9101,\"medioPago\":\"EFECTIVO\",\"importePagado\":250000,\"interes\":-1}",
            "{\"facturaId\":9101,\"medioPago\":\"EFECTIVO\",\"importePagado\":0,\"interes\":0}",
            "{\"facturaId\":9101,\"medioPago\":\"EFECTIVO\",\"importePagado\":250000.001,\"interes\":0}"})
    void datosInvalidosNoPersistenNada(String json) throws Exception {
        mvc.perform(post("/pagos").contentType("application/json").content(json))
                .andExpect(status().isBadRequest());
        assertSinPago(9101);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-10-31", "2026-11-08", "2026-02-30"})
    void fechasInvalidas(String fecha) throws Exception {
        String json = cuerpo(9101, "250000", "0").replace("}", ",\"fechaPago\":\"" + fecha + "\"}");
        mvc.perform(post("/pagos").contentType("application/json").content(json))
                .andExpect(status().isBadRequest());
        assertSinPago(9101);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-11-01", "2026-11-07"})
    void limitesDeFechaPermitidos(String fecha) throws Exception {
        String json = cuerpo(9101, "250000", "0").replace("}", ",\"fechaPago\":\"" + fecha + "\"}");
        mvc.perform(post("/pagos").contentType("application/json").content(json))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.fechaPago").value(fecha));
    }

    @ParameterizedTest
    @ValueSource(strings = {"249999", "250001"})
    void importeDistintoAlTotalEsConflicto(String importe) throws Exception {
        mvc.perform(post("/pagos").contentType("application/json").content(cuerpo(9101, importe, "0")))
                .andExpect(status().isConflict());
        assertSinPago(9101);
    }

    @Test
    void facturaAnuladaNoAdmitePago() throws Exception {
        mvc.perform(post("/pagos").contentType("application/json").content(cuerpo(9103, "250000", "0")))
                .andExpect(status().isConflict());
        assertEquals(0, cantidad(9103));
        assertEquals("ANULADA", estado(9103));
    }

    @Test
    void segundoPagoRechazadoSinDuplicar() throws Exception {
        var request = post("/pagos").contentType("application/json").content(cuerpo(9101, "250000", "0"));
        mvc.perform(request).andExpect(status().isCreated());
        mvc.perform(request).andExpect(status().isConflict());
        assertEquals(1, cantidad(9101));
        assertEquals("PAGADA", estado(9101));
    }

    @ParameterizedTest
    @ValueSource(longs = {9102, 9105})
    void admiteVencidasConInteresSinCambiarImporteBase(long id) throws Exception {
        mvc.perform(post("/pagos").contentType("application/json").content(cuerpo(id, "255000.00", "5000.00")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.interes").value(5000));
        assertEquals("PAGADA", estado(id));
        assertEquals(0, new BigDecimal("250000").compareTo(jdbc.queryForObject(
                "SELECT importe FROM facturas WHERE id=?", BigDecimal.class, id)));
    }

    @Test
    void pagoNoExigeQueElContratoContinueActivo() throws Exception {
        jdbc.update("UPDATE contratos SET estado='FINALIZADO', eliminado=TRUE WHERE id=9001");
        mvc.perform(post("/pagos").contentType("application/json").content(cuerpo(9101, "250000", "0")))
                .andExpect(status().isCreated());
    }

    @Test
    void fallaDespuesDeAmbasEscriturasRevierteTodo() {
        FacturaPagoService target = AopTestUtils.getUltimateTargetObject(facturas);
        doAnswer(invocation -> {
            invocation.callRealMethod(); // Actualizacion real y flush de factura; el pago ya se inserto.
            throw new IllegalStateException("Falla inducida para comprobar rollback");
        }).when(target).marcarPagada(any(Factura.class));

        assertThrows(IllegalStateException.class, () -> service.registrar(solicitud(9101)));
        assertSinPago(9101);
        assertEquals(0L, jdbc.queryForObject("SELECT version FROM facturas WHERE id=9101", Long.class));
    }

    @Test
    void baseDeDatosImpideDosPagosParaLaMismaFactura() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO pagos (factura_id, fecha_pago, medio_pago, importe_pagado, interes)
                VALUES (9104, '2026-11-07', 'EFECTIVO', 250000, 0)
                """));
        assertEquals(1, cantidad(9104));
    }

    @Test
    void dosRegistrosSimultaneosSoloPermitenUnPago() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch inicio = new CountDownLatch(1);
        Callable<String> tarea = () -> {
            inicio.await();
            try {
                service.registrar(solicitud(9101));
                return "creado";
            } catch (ConflictoNegocioException ex) {
                return "conflicto";
            }
        };
        try {
            Future<String> a = executor.submit(tarea);
            Future<String> b = executor.submit(tarea);
            inicio.countDown();
            var resultados = java.util.List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
            assertTrue(resultados.contains("creado"));
            assertTrue(resultados.contains("conflicto"));
            assertEquals(1, cantidad(9101));
            assertEquals("PAGADA", estado(9101));
        } finally {
            executor.shutdownNow();
        }
    }

    private RegistrarPagoRequest solicitud(long facturaId) {
        return new RegistrarPagoRequest(facturaId, null, MedioPago.TRANSFERENCIA,
                new BigDecimal("250000"), BigDecimal.ZERO);
    }

    private String cuerpo(long id, String importe, String interes) {
        return "{\"facturaId\":" + id + ",\"medioPago\":\"TRANSFERENCIA\",\"importePagado\":"
                + importe + ",\"interes\":" + interes + "}";
    }

    private String estado(long id) {
        return jdbc.queryForObject("SELECT estado FROM facturas WHERE id=?", String.class, id);
    }

    private int cantidad(long id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM pagos WHERE factura_id=?", Integer.class, id);
    }

    private void assertSinPago(long id) {
        assertEquals(0, cantidad(id));
        assertEquals("PENDIENTE", estado(id));
    }
}
