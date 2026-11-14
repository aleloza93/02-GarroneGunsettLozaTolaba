package com.tuti.daos.informes.domain;

import com.tuti.daos.incidentes.domain.Incidente;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "informes_resolucion")
public class InformeResolucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incidente_id", nullable = false)
    private Incidente incidente;

    @NotBlank
    @Column(nullable = false)
    private String descripcionTrabajo;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime fechaHoraResolucion;

    @NotNull
    @DecimalMin("0.0")
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal costo;

    @Column
    private String observaciones;

    @NotBlank
    @Column(nullable = false)
    private String responsable;

    protected InformeResolucion() {
        // Constructor requerido por JPA.
    }

    public InformeResolucion(
            Incidente incidente,
            String descripcionTrabajo,
            LocalDateTime fechaHoraResolucion,
            BigDecimal costo,
            String observaciones,
            String responsable
    ) {
        this.incidente = incidente;
        this.descripcionTrabajo = descripcionTrabajo;
        this.fechaHoraResolucion = fechaHoraResolucion;
        this.costo = costo;
        this.observaciones = observaciones;
        this.responsable = responsable;
    }

    public Long getId() {
        return id;
    }

    public Incidente getIncidente() {
        return incidente;
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
