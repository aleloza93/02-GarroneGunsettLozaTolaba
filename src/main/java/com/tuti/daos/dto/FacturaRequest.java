package com.tuti.daos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.*;


public class FacturaRequest {

    @NotNull
    private Long contratoId;

    @NotBlank
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "El período debe tener formato AAAA-MM")
    private String periodo;

    @Size(max = 255)
    private String concepto;

    private LocalDate fechaEmision;
    private LocalDate fechaVencimiento;

    @Positive
    private BigDecimal importe;

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

	public void setContratoId(Long contratoId) {
		this.contratoId = contratoId;
	}

	public void setPeriodo(String periodo) {
		this.periodo = periodo;
	}

	public void setConcepto(String concepto) {
		this.concepto = concepto;
	}

	public void setFechaEmision(LocalDate fechaEmision) {
		this.fechaEmision = fechaEmision;
	}

	public void setFechaVencimiento(LocalDate fechaVencimiento) {
		this.fechaVencimiento = fechaVencimiento;
	}

	public void setImporte(BigDecimal importe) {
		this.importe = importe;
	}

    
}
