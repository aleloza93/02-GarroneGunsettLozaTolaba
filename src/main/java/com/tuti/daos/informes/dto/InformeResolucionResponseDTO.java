package com.tuti.daos.informes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tuti.daos.informes.domain.InformeResolucion;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.RepresentationModel;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Informe registrado al resolver un incidente")
public class InformeResolucionResponseDTO extends RepresentationModel<InformeResolucionResponseDTO> {

    private Long id;
    private Long incidenteId;
    private String descripcionTrabajo;
    private LocalDateTime fechaHoraResolucion;
    private BigDecimal costo;
    private String observaciones;
    private String responsable;

    public InformeResolucionResponseDTO() {
    }

    public static InformeResolucionResponseDTO desde(InformeResolucion informe) {
        InformeResolucionResponseDTO dto = new InformeResolucionResponseDTO();
        dto.id = informe.getId();
        dto.incidenteId = informe.getIncidente().getId();
        dto.descripcionTrabajo = informe.getDescripcionTrabajo();
        dto.fechaHoraResolucion = informe.getFechaHoraResolucion();
        dto.costo = informe.getCosto();
        dto.observaciones = informe.getObservaciones();
        dto.responsable = informe.getResponsable();
        return dto;
    }

    public Long getId() {
        return id;
    }

    public Long getIncidenteId() {
        return incidenteId;
    }

    public String getDescripcionTrabajo() {
        return descripcionTrabajo;
    }

    public LocalDateTime getFechaHoraResolucion() {
        return fechaHoraResolucion;
    }

    public BigDecimal getCosto() {
        return costo;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public String getResponsable() {
        return responsable;
    }
}
