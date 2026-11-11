
package com.tuti.daos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tuti.daos.utilidades.EstadoFactura;

public class FacturaResponse extends RepresentationModel<FacturaResponse> {

    private final Long id;
    private final Long contratoId;
    private final String periodo;
    private final String concepto;
    private final LocalDate fechaEmision;
    private final LocalDate fechaVencimiento;
    private final BigDecimal importe;
    private final EstadoFactura estado;
    private final boolean mensual;
    @JsonIgnore
    private final Long pagoId;

    public FacturaResponse(Long id, Long contratoId, String periodo, String concepto,
                           LocalDate fechaEmision, LocalDate fechaVencimiento,
                           BigDecimal importe, EstadoFactura estado, boolean mensual, Long pagoId) {
        this.id = id; this.contratoId = contratoId; this.periodo = periodo;
        this.concepto = concepto; this.fechaEmision = fechaEmision;
        this.fechaVencimiento = fechaVencimiento; this.importe = importe;
        this.estado = estado; this.mensual = mensual; this.pagoId = pagoId;
    }

	public Long getId() {
		return id;
	}

	public Long getContratoId() {
		return contratoId;
	}

	public String getPeriodo() {
		return periodo;
	}

	public String getConcepto() {
		return concepto;
	}

	public LocalDate getFechaEmision() {
		return fechaEmision;
	}

	public LocalDate getFechaVencimiento() {
		return fechaVencimiento;
	}

	public BigDecimal getImporte() {
		return importe;
	}

	public EstadoFactura getEstado() {
		return estado;
	}

	public boolean isMensual() {
		return mensual;
	}

	public Long getPagoId() {
		return pagoId;
	}
    
    
    
}
