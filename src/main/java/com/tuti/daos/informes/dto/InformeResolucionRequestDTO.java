package com.tuti.daos.informes.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Datos necesarios para registrar la resolución de un incidente")
public class InformeResolucionRequestDTO {

    @NotNull(message = "El incidenteId es obligatorio")
    @Schema(description = "Identificador del incidente que se resolverá", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long incidenteId;

    @NotBlank(message = "La descripcionTrabajo es obligatoria")
    @Schema(description = "Descripción del trabajo realizado", example = "Reparación de cañería principal", requiredMode = Schema.RequiredMode.REQUIRED)
    private String descripcionTrabajo;

    @NotNull(message = "El costo es obligatorio")
    @DecimalMin(value = "0.0", message = "El costo debe ser mayor o igual a cero")
    @Schema(description = "Costo total de la resolución; debe ser mayor o igual a cero", example = "15000.50", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal costo;

    @Schema(description = "Observaciones opcionales", example = "Trabajo finalizado en tiempo y forma")
    private String observaciones;

    @NotBlank(message = "El responsable es obligatorio")
    @Schema(description = "Nombre del técnico o responsable", example = "Juan Pérez - Plomero", requiredMode = Schema.RequiredMode.REQUIRED)
    private String responsable;

    public InformeResolucionRequestDTO() {
    }

    public Long getIncidenteId() {
        return incidenteId;
    }

    public void setIncidenteId(Long incidenteId) {
        this.incidenteId = incidenteId;
    }

    public String getDescripcionTrabajo() {
        return descripcionTrabajo;
    }

    public void setDescripcionTrabajo(String descripcionTrabajo) {
        this.descripcionTrabajo = descripcionTrabajo;
    }

    public BigDecimal getCosto() {
        return costo;
    }

    public void setCosto(BigDecimal costo) {
        this.costo = costo;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public String getResponsable() {
        return responsable;
    }

    public void setResponsable(String responsable) {
        this.responsable = responsable;
    }
}
