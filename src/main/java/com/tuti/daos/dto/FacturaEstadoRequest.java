package com.tuti.daos.dto;

import com.tuti.daos.utilidades.EstadoFactura;

import jakarta.validation.constraints.NotNull;

public class FacturaEstadoRequest {

    @NotNull
    private EstadoFactura estado;

	public EstadoFactura getEstado() {
		return estado;
	}

	public void setEstado(EstadoFactura estado) {
		this.estado = estado;
	}

}
