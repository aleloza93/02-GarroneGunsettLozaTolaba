package com.tuti.common;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import com.tuti.facturas.dto.EmitirFacturaRequest;
import com.tuti.pagos.dto.RegistrarPagoRequest;
import com.tuti.pagos.model.MedioPago;

class ContratosEntradaTests {
    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    @AfterAll
    static void cerrar() { FACTORY.close(); }

    @Test
    void facturaMensualAdmiteSoloContratoYPeriodo() {
        assertTrue(VALIDATOR.validate(new EmitirFacturaRequest(9001L, "2026-11",
                null, null, null, null)).isEmpty());
    }

    @Test
    void facturaRechazaMesInvalido() {
        assertFalse(VALIDATOR.validate(new EmitirFacturaRequest(9001L, "2026-13",
                null, null, null, null)).isEmpty());
    }

    @Test
    void pagoAdmiteInteresCeroYFechaOmitida() {
        assertTrue(VALIDATOR.validate(pago("250000.00", "0.00")).isEmpty());
    }

    @Test
    void pagoRequiereInteresAunqueSeaCero() {
        assertFalse(VALIDATOR.validate(new RegistrarPagoRequest(1L, null,
                MedioPago.EFECTIVO, new BigDecimal("250000.00"), null)).isEmpty());
    }

    @Test
    void pagoRechazaInteresNegativo() {
        assertFalse(VALIDATOR.validate(pago("250000.00", "-1.00")).isEmpty());
    }

    @Test
    void pagoRechazaImporteCero() {
        assertFalse(VALIDATOR.validate(pago("0.00", "0.00")).isEmpty());
    }

    @Test
    void propuestaMonetariaRechazaMasDeDosDecimales() {
        assertFalse(VALIDATOR.validate(pago("250000.001", "0.00")).isEmpty());
    }

    @Test
    void camposObligatoriosFaltantes() {
        assertEquals(4, VALIDATOR.validate(new RegistrarPagoRequest(null, null,
                null, null, null)).size());
    }

    private RegistrarPagoRequest pago(String importe, String interes) {
        return new RegistrarPagoRequest(1L, null, MedioPago.TRANSFERENCIA,
                new BigDecimal(importe), new BigDecimal(interes));
    }
}

