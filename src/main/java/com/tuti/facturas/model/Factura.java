package com.tuti.facturas.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import jakarta.persistence.*;
import com.tuti.contratos.model.Contrato;
import com.tuti.common.exception.ConflictoNegocioException;

/**
 * Modelo compartido acordado para S01/S02. Las reglas de emision y anulacion
 * corresponden a S01; este modelo no expone operaciones REST.
 */
@Entity
@Table(name = "facturas")
public class Factura {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contrato_id", nullable = false)
    private Contrato contrato;

    @Convert(converter = PeriodoConverter.class)
    @Column(nullable = false, length = 7)
    private YearMonth periodo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoFactura tipo;

    @Column(columnDefinition = "TEXT")
    private String concepto;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal importe;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoFactura estado;

    // Si alguien actualizo la factura antes que nosotros, JPA detecta la version desactualizada.
    @Version
    @Column(nullable = false)
    private Long version;

    protected Factura() {}

    public Long getId() { return id; }
    public Contrato getContrato() { return contrato; }
    public YearMonth getPeriodo() { return periodo; }
    public TipoFactura getTipo() { return tipo; }
    public String getConcepto() { return concepto; }
    public LocalDate getFechaEmision() { return fechaEmision; }
    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public BigDecimal getImporte() { return importe; }
    public EstadoFactura getEstado() { return estado; }
    public Long getVersion() { return version; }

    public EstadoFactura estadoEn(LocalDate fecha) {
        // Calculamos si vencio sin tocar el importe ni necesitar una tarea programada.
        if (estado == EstadoFactura.PAGADA || estado == EstadoFactura.ANULADA) {
            return estado;
        }
        return fecha.isAfter(fechaVencimiento) ? EstadoFactura.VENCIDA : EstadoFactura.PENDIENTE;
    }

    public void verificarPagable() {
        if (estado != EstadoFactura.PENDIENTE && estado != EstadoFactura.VENCIDA) {
            throw new ConflictoNegocioException("La factura debe estar pendiente o vencida.");
        }
    }

    // La marcamos pagada desde el registro del pago, no desde un cambio de estado suelto.
    public void marcarPagada() {
        verificarPagable();
        estado = EstadoFactura.PAGADA;
    }
}
