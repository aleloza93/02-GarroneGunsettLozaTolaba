package com.tuti.contratos.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.persistence.*;

@Entity
@Table(name = "contratos")
public class Contrato {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "duracion_meses", nullable = false)
    private Integer duracionMeses;

    @Column(name = "importe_mensual", nullable = false, precision = 15, scale = 2)
    private BigDecimal importeMensual;

    @Column(name = "dia_vencimiento", nullable = false)
    private Integer diaVencimiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoContrato estado;

    @Column(nullable = false)
    private boolean eliminado;

    protected Contrato() {}

    public Long getId() { return id; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public Integer getDuracionMeses() { return duracionMeses; }
    public BigDecimal getImporteMensual() { return importeMensual; }
    public Integer getDiaVencimiento() { return diaVencimiento; }
    public EstadoContrato getEstado() { return estado; }
    public boolean isEliminado() { return eliminado; }

    public LocalDate getFechaFin() {
        return fechaInicio.plusMonths(duracionMeses).minusDays(1);
    }

    public boolean estaVigenteEn(LocalDate fecha) {
        return !fecha.isBefore(fechaInicio) && !fecha.isAfter(getFechaFin());
    }
}

