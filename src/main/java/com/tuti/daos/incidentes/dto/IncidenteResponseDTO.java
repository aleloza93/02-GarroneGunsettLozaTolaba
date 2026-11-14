package com.tuti.daos.incidentes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tuti.daos.incidentes.domain.CategoriaIncidente;
import com.tuti.daos.incidentes.domain.Incidente;
import com.tuti.daos.incidentes.domain.PrioridadIncidente;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.RepresentationModel;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Información completa de un incidente de mantenimiento")
public class IncidenteResponseDTO extends RepresentationModel<IncidenteResponseDTO> {

    private Long id;
    private Long contratoId;
    private String titulo;
    private String descripcion;
    private CategoriaIncidente categoria;
    private LocalDateTime fechaHoraAlta;
    private PrioridadIncidente prioridad;
    private String estado;
    private List<HistorialEstadoIncidenteResponseDTO> historialEstados;

    public static IncidenteResponseDTO desde(Incidente incidente) {
        IncidenteResponseDTO dto = new IncidenteResponseDTO();
        dto.id = incidente.getId();
        dto.contratoId = incidente.getContratoId();
        dto.titulo = incidente.getTitulo();
        dto.descripcion = incidente.getDescripcion();
        dto.categoria = incidente.getCategoria();
        dto.fechaHoraAlta = incidente.getFechaHoraAlta();
        dto.prioridad = incidente.getPrioridad();
        dto.estado = incidente.getEstado().getNombre();
        dto.historialEstados = incidente.getHistorialEstados().stream()
                .map(HistorialEstadoIncidenteResponseDTO::desde)
                .toList();
        return dto;
    }

    public Long getId() {
        return id;
    }

    public Long getContratoId() {
        return contratoId;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public CategoriaIncidente getCategoria() {
        return categoria;
    }

    public LocalDateTime getFechaHoraAlta() {
        return fechaHoraAlta;
    }

    public PrioridadIncidente getPrioridad() {
        return prioridad;
    }

    public String getEstado() {
        return estado;
    }

    public List<HistorialEstadoIncidenteResponseDTO> getHistorialEstados() {
        return historialEstados;
    }
}
