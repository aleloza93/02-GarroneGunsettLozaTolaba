package com.tuti.daos.incidentes.domain;

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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "incidentes")
public class Incidente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El contrato del incidente es obligatorio")
    @Column(nullable = false)
    private Long contratoId;

    @NotBlank(message = "El título del incidente es obligatorio")
    @Column(nullable = false)
    private String titulo;

    @NotBlank(message = "La descripción del incidente es obligatoria")
    @Column(nullable = false)
    private String descripcion;

    @NotNull(message = "La categoría del incidente es obligatoria")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoriaIncidente categoria;

    @NotNull(message = "La fecha de alta del incidente es obligatoria")
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaHoraAlta;

    @NotNull(message = "La prioridad del incidente es obligatoria")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PrioridadIncidente prioridad;

    @NotNull(message = "El estado del incidente es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estado_id", nullable = false)
    private EstadoIncidente estado;

    @OneToMany(mappedBy = "incidente", fetch = FetchType.LAZY)
    @OrderBy("fechaHoraCambio ASC")
    private List<HistorialEstadoIncidente> historialEstados = new ArrayList<>();

    protected Incidente() {
        // Constructor requerido por JPA.
    }

    public Incidente(
            Long contratoId,
            String titulo,
            String descripcion,
            CategoriaIncidente categoria,
            PrioridadIncidente prioridad,
            EstadoIncidente estado
    ) {
        this.contratoId = contratoId;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.categoria = categoria;
        this.prioridad = prioridad;
        this.estado = estado;
    }

    @PrePersist
    void asignarFechaHoraAlta() {
        if (fechaHoraAlta == null) {
            fechaHoraAlta = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getContratoId() {
        return contratoId;
    }

    public void setContratoId(Long contratoId) {
        this.contratoId = contratoId;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public CategoriaIncidente getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaIncidente categoria) {
        this.categoria = categoria;
    }

    public LocalDateTime getFechaHoraAlta() {
        return fechaHoraAlta;
    }

    public PrioridadIncidente getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(PrioridadIncidente prioridad) {
        this.prioridad = prioridad;
    }

    public EstadoIncidente getEstado() {
        return estado;
    }

    public void setEstado(EstadoIncidente estado) {
        this.estado = estado;
    }

    public List<HistorialEstadoIncidente> getHistorialEstados() {
        return historialEstados;
    }
}
