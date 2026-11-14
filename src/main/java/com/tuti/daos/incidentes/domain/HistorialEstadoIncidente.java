package com.tuti.daos.incidentes.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "historial_estados_incidente")
public class HistorialEstadoIncidente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El incidente del historial es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incidente_id", nullable = false)
    private Incidente incidente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_anterior_id")
    private EstadoIncidente estadoAnterior;

    @NotNull(message = "El nuevo estado es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estado_nuevo_id", nullable = false)
    private EstadoIncidente estadoNuevo;

    @NotNull(message = "La fecha del cambio de estado es obligatoria")
    @Column(nullable = false)
    private LocalDateTime fechaHoraCambio;

    @Column(length = 1000)
    private String observaciones;

    protected HistorialEstadoIncidente() {
        // Constructor requerido por JPA.
    }

    public HistorialEstadoIncidente(
            Incidente incidente,
            EstadoIncidente estadoAnterior,
            EstadoIncidente estadoNuevo,
            LocalDateTime fechaHoraCambio,
            String observaciones
    ) {
        this.incidente = incidente;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.fechaHoraCambio = fechaHoraCambio;
        this.observaciones = observaciones;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Incidente getIncidente() {
        return incidente;
    }

    public void setIncidente(Incidente incidente) {
        this.incidente = incidente;
    }

    public EstadoIncidente getEstadoAnterior() {
        return estadoAnterior;
    }

    public void setEstadoAnterior(EstadoIncidente estadoAnterior) {
        this.estadoAnterior = estadoAnterior;
    }

    public EstadoIncidente getEstadoNuevo() {
        return estadoNuevo;
    }

    public void setEstadoNuevo(EstadoIncidente estadoNuevo) {
        this.estadoNuevo = estadoNuevo;
    }

    public LocalDateTime getFechaHoraCambio() {
        return fechaHoraCambio;
    }

    public void setFechaHoraCambio(LocalDateTime fechaHoraCambio) {
        this.fechaHoraCambio = fechaHoraCambio;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}
