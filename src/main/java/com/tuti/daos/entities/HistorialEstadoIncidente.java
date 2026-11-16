package com.tuti.daos.entities;

import java.time.LocalDateTime;

import com.tuti.daos.utilidades.EstadoIncidente;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "historial_estados_incidente")
public class HistorialEstadoIncidente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incidente_id", nullable = false)
    private Incidente incidente;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 20)
    private EstadoIncidente estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 20)
    private EstadoIncidente estadoNuevo;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    public HistorialEstadoIncidente() {
    }

    public HistorialEstadoIncidente(Incidente incidente, EstadoIncidente estadoAnterior,
            EstadoIncidente estadoNuevo, LocalDateTime fechaHora) {
        this.incidente = incidente;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.fechaHora = fechaHora;
    }

    public Long getId() {
        return id;
    }

    public Incidente getIncidente() {
        return incidente;
    }

    public EstadoIncidente getEstadoAnterior() {
        return estadoAnterior;
    }

    public EstadoIncidente getEstadoNuevo() {
        return estadoNuevo;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }
}
