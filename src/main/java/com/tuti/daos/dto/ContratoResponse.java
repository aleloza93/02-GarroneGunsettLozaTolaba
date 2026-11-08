package com.tuti.daos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.tuti.daos.entities.Contrato;
import com.tuti.daos.utilidades.EstadoContrato;

public record ContratoResponse(Long id, LocalDate fechaInicio, Integer duracionMeses,
        LocalDate fechaFin, BigDecimal importeMensual, Integer diaVencimiento,
        EstadoContrato estado, boolean eliminado) {
    public static ContratoResponse desde(Contrato contrato) {
        return new ContratoResponse(contrato.getId(), contrato.getFechaInicio(),
                contrato.getDuracionMeses(), contrato.getFechaFin(),
                contrato.getImporteMensual(), contrato.getDiaVencimiento(),
                contrato.getEstado(), contrato.isEliminado());
    }
}

